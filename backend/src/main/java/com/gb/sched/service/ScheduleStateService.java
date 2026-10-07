package com.gb.sched.service;

import com.gb.sched.config.AppConstants;
import com.gb.sched.model.AdjustShiftRequest;
import com.gb.sched.model.ScheduleItem;
import com.gb.sched.model.ShiftAdjustment;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 排班表的权威内存状态。
 *
 * <p>排班 = 引擎基线 + 调整记录套用；调整记录是手动换班的唯一权威来源。
 * 一键生成采用方案三：按「人 × 日期」保住调整记录里改过的格子，引擎只重铺没动过的格子。
 */
@Service
public class ScheduleStateService {

  private record DepartmentState(List<ScheduleItem> schedule, List<ShiftAdjustment> adjustments, long adjustmentSeq) {}

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final Map<String, DepartmentState> states = new HashMap<>();
  private final ScheduleRuleService scheduleRuleService;

  public ScheduleStateService(ScheduleRuleService scheduleRuleService) {
    this.scheduleRuleService = scheduleRuleService;
  }

  /** 获取当前排班（已套用调整、已重跑冲突检测）。 */
  public synchronized List<ScheduleItem> currentSchedule(String department) {
    return new ArrayList<>(state(department).schedule());
  }

  public synchronized List<ShiftAdjustment> adjustments(String department) {
    return new ArrayList<>(state(department).adjustments());
  }

  /**
   * 护士长直接在排班表上给某人换班次。
   * 换完后对该科室整张表重跑「夜班后接白班」「连续上班超 5 天」检测，
   * 排班表格子、冲突提示、工时统计都从换过的结果重新派生。
   */
  public synchronized List<ScheduleItem> adjustShift(AdjustShiftRequest request) {
    validate(request);

    DepartmentState state = state(request.department());
    List<ScheduleItem> schedule = new ArrayList<>(state.schedule());
    int index = findItemIndex(schedule, request.staffName(), request.date());
    if (index < 0) {
      throw new IllegalArgumentException("排班表中不存在该人员当天的班次");
    }

    ScheduleItem target = schedule.get(index);
    if (target.shift().equals(request.newShift())) {
      // 班次没变化：不产生调整记录，直接返回当前结果
      return new ArrayList<>(state.schedule());
    }

    String reason = request.reason() == null || request.reason().isBlank() ? "护士长手动调整" : request.reason().trim();
    long nextId = state.adjustmentSeq() + 1;
    ShiftAdjustment adjustment = new ShiftAdjustment(
        nextId,
        request.department(),
        request.date(),
        request.staffName(),
        target.shift(),
        request.newShift(),
        reason,
        AppConstants.DEFAULT_OPERATOR,
        LocalDateTime.now().format(TIME_FORMAT));

    // 直接改权威排班中的格子，标记 adjusted；再重跑冲突检测
    schedule.set(index, target.withShift(
        request.newShift(),
        ScheduleRuleService.shiftColor(request.newShift()),
        true));
    List<ScheduleItem> rechecked = scheduleRuleService.detectConflicts(schedule);

    List<ShiftAdjustment> adjustments = new ArrayList<>(state.adjustments());
    adjustments.add(adjustment);
    adjustments.sort(Comparator.comparing(ShiftAdjustment::createdAt).reversed());
    states.put(request.department(), new DepartmentState(rechecked, adjustments, nextId));
    return new ArrayList<>(rechecked);
  }

  /**
   * 一键生成（方案三）：引擎照原样重铺基线 → 把每条调整记录按「人 × 日期」套回 → 重跑冲突。
   * 改过的格子被保住，没动过的格子只反映引擎最新结果；
   * 同一天同一个人的班次与调整记录永远一致。
   */
  public synchronized List<ScheduleItem> regenerate(String department) {
    DepartmentState state = state(department);

    // 1. 引擎重铺原始基线
    List<ScheduleItem> rebuilt = new ArrayList<>(scheduleRuleService.generateBaseline(department));

    // 2. 按「人 × 日期」套回所有调整记录，只盖没动过的格子
    for (ShiftAdjustment adjustment : state.adjustments()) {
      int index = findItemIndex(rebuilt, adjustment.staffName(), adjustment.date());
      if (index >= 0) {
        rebuilt.set(index, rebuilt.get(index).withShift(
            adjustment.newShift(),
            ScheduleRuleService.shiftColor(adjustment.newShift()),
            true));
      }
    }

    // 3. 改过的结果重看冲突
    List<ScheduleItem> rechecked = scheduleRuleService.detectConflicts(rebuilt);
    states.put(department, new DepartmentState(rechecked, state.adjustments(), state.adjustmentSeq()));
    return new ArrayList<>(rechecked);
  }

  private DepartmentState state(String department) {
    return states.computeIfAbsent(department, dept -> {
      List<ScheduleItem> baseline = scheduleRuleService.generateBaseline(dept);
      List<ScheduleItem> checked = scheduleRuleService.detectConflicts(baseline);
      return new DepartmentState(checked, new ArrayList<>(), 0L);
    });
  }

  private void validate(AdjustShiftRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("请求体不能为空");
    }
    if (isBlank(request.department()) || isBlank(request.date()) || isBlank(request.staffName())) {
      throw new IllegalArgumentException("科室、日期、人员不能为空");
    }
    if (!AppConstants.SHIFT_TYPES.contains(request.newShift())) {
      throw new IllegalArgumentException("新班次必须是：白班、中班、夜班、休息");
    }
  }

  private int findItemIndex(List<ScheduleItem> schedule, String staffName, String date) {
    for (int i = 0; i < schedule.size(); i++) {
      ScheduleItem item = schedule.get(i);
      if (item.staffName().equals(staffName) && item.date().equals(date)) {
        return i;
      }
    }
    return -1;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

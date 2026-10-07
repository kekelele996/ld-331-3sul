package com.gb.sched.service;

import com.gb.sched.config.AppConstants;
import com.gb.sched.model.AdjustmentRecord;
import com.gb.sched.model.ScheduleItem;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

/**
 * 排班数据存储（内存版）。
 *
 * <p>一键生成口径：手动调整过的「日期+人」格子锁定保留，引擎重铺只覆盖未动过的格子，
 * 因此排班表始终与调整记录一致。
 */
@Service
public class ScheduleStore {
  private static final String OPERATOR = "护士长";

  private final ScheduleRuleService ruleService;
  private final Map<String, List<ScheduleItem>> schedules = new ConcurrentHashMap<>();
  private final Map<String, Map<String, String>> overrides = new ConcurrentHashMap<>();
  private final Map<String, List<AdjustmentRecord>> adjustmentLogs = new ConcurrentHashMap<>();
  private final AtomicInteger idSequence = new AtomicInteger(1);

  public ScheduleStore(ScheduleRuleService ruleService) {
    this.ruleService = ruleService;
  }

  public synchronized List<ScheduleItem> scheduleFor(String department) {
    return schedules.computeIfAbsent(department, ruleService::generateMonthlySchedule);
  }

  public synchronized ScheduleItem adjust(String department, String date, String staffName, String shift) {
    if (!AppConstants.SHIFT_TYPES.contains(shift)) {
      throw new IllegalArgumentException("非法班次：" + shift);
    }
    List<ScheduleItem> items = scheduleFor(department);
    for (int i = 0; i < items.size(); i++) {
      ScheduleItem item = items.get(i);
      if (item.date().equals(date) && item.staffName().equals(staffName)) {
        String oldShift = item.shift();
        items.set(i, item.withShift(shift, ruleService.shiftColor(shift)));
        overrides.computeIfAbsent(department, key -> new ConcurrentHashMap<>()).put(key(date, staffName), shift);
        adjustmentLogs
            .computeIfAbsent(department, key -> new ArrayList<>())
            .add(new AdjustmentRecord(
                idSequence.getAndIncrement(),
                date,
                staffName,
                oldShift,
                shift,
                OPERATOR,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        return items.get(i);
      }
    }
    throw new IllegalArgumentException("未找到 " + date + " " + staffName + " 的排班记录");
  }

  public synchronized List<ScheduleItem> regenerate(String department) {
    List<ScheduleItem> fresh = ruleService.generateMonthlySchedule(department);
    Map<String, String> locked = overrides.getOrDefault(department, Map.of());
    List<ScheduleItem> merged = new ArrayList<>(fresh.size());
    for (ScheduleItem item : fresh) {
      String keptShift = locked.get(key(item.date(), item.staffName()));
      merged.add(keptShift == null ? item : item.withShift(keptShift, ruleService.shiftColor(keptShift)));
    }
    schedules.put(department, merged);
    return merged;
  }

  public List<AdjustmentRecord> adjustmentsFor(String department) {
    return adjustmentLogs.getOrDefault(department, List.of());
  }

  private String key(String date, String staffName) {
    return date + "|" + staffName;
  }
}

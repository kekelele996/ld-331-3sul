package com.gb.sched.service;

import com.gb.sched.config.AppConstants;
import com.gb.sched.model.ConflictAlert;
import com.gb.sched.model.ScheduleItem;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ScheduleRuleService {

  public record StaffMember(String name, String position) {}

  public static final List<StaffMember> STAFF = List.of(
      new StaffMember("陈医生", "医生"),
      new StaffMember("林医生", "医生"),
      new StaffMember("周护士", "护士"),
      new StaffMember("赵护士", "护士"),
      new StaffMember("王护士", "护士"));

  /** 排班周期：当月 1 日起 14 天。 */
  public static final int SCHEDULE_DAYS = 14;

  /**
   * 引擎基线排班（不含任何手动调整）。手动调整的格子由 ScheduleStateService
   * 在基线之上套用调整记录，保证调整记录是唯一权威来源。
   */
  public List<ScheduleItem> generateBaseline(String department) {
    List<ScheduleItem> items = new ArrayList<>();
    LocalDate start = LocalDate.now().withDayOfMonth(1);
    List<String> shiftTypes = AppConstants.SHIFT_TYPES;
    for (int day = 0; day < SCHEDULE_DAYS; day++) {
      LocalDate current = start.plusDays(day);
      for (int index = 0; index < STAFF.size(); index++) {
        StaffMember member = STAFF.get(index);
        String shift = shiftTypes.get((day + index) % shiftTypes.size());
        items.add(new ScheduleItem(
            current.toString(),
            department,
            member.position(),
            member.name(),
            shift,
            day % 6 == 0,
            shiftColor(shift),
            false,
            ScheduleItem.CONFLICT_NONE));
      }
    }
    return items;
  }

  /**
   * 对当前排班重跑冲突检测，并把冲突类型回填到每个格子上：
   * 1. 夜班后接白班（前一天夜班、当天白班，日期必须相邻）；
   * 2. 连续上班超过 {@link AppConstants#MAX_CONTINUOUS_WORK_DAYS} 天（休息打断连续，休息不算）。
   */
  public List<ScheduleItem> detectConflicts(List<ScheduleItem> schedule) {
    Map<String, ScheduleItem> keyed = new HashMap<>();
    for (ScheduleItem item : schedule) {
      keyed.put(key(item.staffName(), item.date()), item);
    }

    List<ScheduleItem> result = new ArrayList<>();
    for (ScheduleItem item : schedule) {
      String conflict = ScheduleItem.CONFLICT_NONE;

      // 规则一：夜班后接白班
      LocalDate currentDate = LocalDate.parse(item.date());
      ScheduleItem previous = keyed.get(key(item.staffName(), currentDate.minusDays(1).toString()));
      if (previous != null
          && "白班".equals(item.shift())
          && "夜班".equals(previous.shift())) {
        conflict = ScheduleItem.CONFLICT_NIGHT_TO_DAY;
      }

      result.add(item.withConflict(conflict));
    }

    // 规则二：连续上班超过 5 天（休息打断，休息那天不算连续）
    Map<String, List<ScheduleItem>> byStaff = new LinkedHashMap<>();
    for (ScheduleItem item : result) {
      byStaff.computeIfAbsent(item.staffName(), name -> new ArrayList<>()).add(item);
    }
    List<ScheduleItem> flagged = new ArrayList<>();
    for (List<ScheduleItem> staffItems : byStaff.values()) {
      staffItems.sort(Comparator.comparing(ScheduleItem::date));
      int streak = 0;
      LocalDate lastDate = null;
      for (ScheduleItem item : staffItems) {
        LocalDate itemDate = LocalDate.parse(item.date());
        boolean adjacent = lastDate != null && itemDate.minusDays(1).equals(lastDate);
        if ("休息".equals(item.shift())) {
          streak = 0; // 休息打断连续，且不计入连续天数
        } else {
          streak = adjacent ? streak + 1 : 1;
          if (streak > AppConstants.MAX_CONTINUOUS_WORK_DAYS) {
            flagged.add(item);
          }
        }
        lastDate = itemDate;
      }
    }

    Map<String, String> flaggedKeys = new HashMap<>();
    for (ScheduleItem item : flagged) {
      flaggedKeys.put(key(item.staffName(), item.date()), ScheduleItem.CONFLICT_CONTINUOUS_DAYS);
    }
    List<ScheduleItem> merged = new ArrayList<>();
    for (ScheduleItem item : result) {
      String override = flaggedKeys.get(key(item.staffName(), item.date()));
      merged.add(override != null ? item.withConflict(override) : item);
    }
    return merged;
  }

  /** 由带冲突标记的排班生成冲突提示列表。 */
  public List<ConflictAlert> toAlerts(List<ScheduleItem> schedule) {
    List<ConflictAlert> alerts = new ArrayList<>();
    for (ScheduleItem item : schedule) {
      switch (item.conflict()) {
        case ScheduleItem.CONFLICT_NIGHT_TO_DAY -> alerts.add(
            new ConflictAlert("warning", item.staffName(), item.date(), AppConstants.CONFLICT_NIGHT_TO_DAY));
        case ScheduleItem.CONFLICT_CONTINUOUS_DAYS -> alerts.add(
            new ConflictAlert("danger", item.staffName(), item.date(), AppConstants.CONFLICT_MAX_CONTINUOUS_DAYS));
        default -> { }
      }
    }
    alerts.sort(Comparator.comparing(ConflictAlert::date).thenComparing(ConflictAlert::staffName));
    return alerts;
  }

  public static String shiftColor(String shift) {
    return AppConstants.SHIFT_COLORS.getOrDefault(shift, "#909399");
  }

  private static String key(String staffName, String date) {
    return staffName + "@" + date;
  }
}

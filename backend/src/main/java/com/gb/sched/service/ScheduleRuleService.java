package com.gb.sched.service;

import com.gb.sched.config.AppConstants;
import com.gb.sched.model.ConflictAlert;
import com.gb.sched.model.ScheduleItem;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ScheduleRuleService {
  private static final List<String> STAFF = List.of("陈医生", "林医生", "周护士", "赵护士", "王护士");
  private static final int MAX_CONTINUOUS_WORK_DAYS = 5;

  public List<ScheduleItem> generateMonthlySchedule(String department) {
    List<ScheduleItem> items = new ArrayList<>();
    LocalDate start = LocalDate.now().withDayOfMonth(1);
    for (int day = 0; day < 14; day++) {
      LocalDate current = start.plusDays(day);
      for (int index = 0; index < STAFF.size(); index++) {
        String shift = AppConstants.SHIFT_TYPES.get((day + index) % AppConstants.SHIFT_TYPES.size());
        items.add(new ScheduleItem(current.toString(), department, index < 2 ? "医生" : "护士", STAFF.get(index), shift, day % 6 == 0, shiftColor(shift)));
      }
    }
    return items;
  }

  /**
   * 冲突检测：夜班后接白班（warning）、连续上班超过 5 天（danger，休息日中断连续）。
   */
  public List<ConflictAlert> detectConflicts(List<ScheduleItem> schedule) {
    List<ConflictAlert> alerts = new ArrayList<>();
    for (Map.Entry<String, List<ScheduleItem>> entry : groupByStaff(schedule).entrySet()) {
      List<ScheduleItem> items = entry.getValue();
      for (int i = 1; i < items.size(); i++) {
        ScheduleItem prev = items.get(i - 1);
        ScheduleItem curr = items.get(i);
        if (isNextDay(prev.date(), curr.date()) && "夜班".equals(prev.shift()) && "白班".equals(curr.shift())) {
          alerts.add(new ConflictAlert("warning", curr.staffName(), curr.date(), AppConstants.CONFLICT_NIGHT_TO_DAY));
        }
      }
      int streak = 0;
      String prevDate = null;
      for (ScheduleItem item : items) {
        boolean working = !"休息".equals(item.shift());
        if (working && prevDate != null && isNextDay(prevDate, item.date())) {
          streak++;
        } else {
          streak = working ? 1 : 0;
        }
        prevDate = item.date();
        if (streak == MAX_CONTINUOUS_WORK_DAYS + 1) {
          alerts.add(new ConflictAlert("danger", item.staffName(), item.date(),
              AppConstants.CONFLICT_MAX_CONTINUOUS_DAYS + "（已连续上班 " + streak + " 天）"));
        }
      }
    }
    alerts.sort(Comparator.comparing(ConflictAlert::date).thenComparing(ConflictAlert::staffName));
    return alerts;
  }

  /**
   * 标出连续上班超过 5 天的班次格子（休息日不算连续），返回带 overLimit 标记的新列表。
   */
  public List<ScheduleItem> markContinuousOverLimit(List<ScheduleItem> schedule) {
    List<ScheduleItem> result = new ArrayList<>(schedule);
    for (List<ScheduleItem> items : groupByStaff(schedule).values()) {
      int streak = 0;
      String prevDate = null;
      for (ScheduleItem item : items) {
        boolean working = !"休息".equals(item.shift());
        if (working && prevDate != null && isNextDay(prevDate, item.date())) {
          streak++;
        } else {
          streak = working ? 1 : 0;
        }
        prevDate = item.date();
        if (streak > MAX_CONTINUOUS_WORK_DAYS) {
          int index = result.indexOf(item);
          if (index >= 0) {
            result.set(index, item.withOverLimit(true));
          }
        }
      }
    }
    return result;
  }

  public String shiftColor(String shift) {
    return switch (shift) {
      case "白班" -> "#409eff";
      case "中班" -> "#67c23a";
      case "夜班" -> "#626aef";
      default -> "#909399";
    };
  }

  private Map<String, List<ScheduleItem>> groupByStaff(List<ScheduleItem> schedule) {
    Map<String, List<ScheduleItem>> byStaff = new LinkedHashMap<>();
    for (ScheduleItem item : schedule) {
      byStaff.computeIfAbsent(item.staffName(), key -> new ArrayList<>()).add(item);
    }
    byStaff.values().forEach(items -> items.sort(Comparator.comparing(ScheduleItem::date)));
    return byStaff;
  }

  private boolean isNextDay(String prevDate, String currDate) {
    return LocalDate.parse(prevDate).plusDays(1).toString().equals(currDate);
  }
}

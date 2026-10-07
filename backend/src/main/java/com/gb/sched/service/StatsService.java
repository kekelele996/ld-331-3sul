package com.gb.sched.service;

import com.gb.sched.model.ScheduleItem;
import com.gb.sched.model.WorkStats;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StatsService {

  /**
   * 依据当前排班（已含手动调整结果）统计工时。
   * 加班小时按班次折算：白班 0、中班 0、夜班 4 小时（夜班强度高，按 1.5 个班次折算演示）。
   */
  public List<WorkStats> monthlyStats(List<ScheduleItem> schedule) {
    Map<String, int[]> counters = new LinkedHashMap<>();
    for (ScheduleItem item : schedule) {
      // index: 0 白班, 1 中班, 2 夜班, 3 休息
      int[] counter = counters.computeIfAbsent(item.staffName(), name -> new int[4]);
      switch (item.shift()) {
        case "白班" -> counter[0]++;
        case "中班" -> counter[1]++;
        case "夜班" -> counter[2]++;
        case "休息" -> counter[3]++;
        default -> { }
      }
    }

    List<WorkStats> stats = new ArrayList<>();
    for (Map.Entry<String, int[]> entry : counters.entrySet()) {
      int[] counter = entry.getValue();
      int workDays = counter[0] + counter[1] + counter[2];
      int overtimeHours = counter[2] * 4;
      stats.add(new WorkStats(entry.getKey(), counter[0], counter[1], counter[2], counter[3], workDays, overtimeHours));
    }
    return stats;
  }
}

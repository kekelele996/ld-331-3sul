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
   * 按当前排班表统计每人各类班次数量；夜班按 2 小时折算加班。
   */
  public List<WorkStats> monthlyStats(List<ScheduleItem> schedule) {
    Map<String, int[]> counter = new LinkedHashMap<>();
    for (ScheduleItem item : schedule) {
      int[] counts = counter.computeIfAbsent(item.staffName(), key -> new int[4]);
      switch (item.shift()) {
        case "白班" -> counts[0]++;
        case "中班" -> counts[1]++;
        case "夜班" -> counts[2]++;
        default -> counts[3]++;
      }
    }
    List<WorkStats> stats = new ArrayList<>();
    counter.forEach((staffName, counts) ->
        stats.add(new WorkStats(staffName, counts[0], counts[1], counts[2], counts[3], counts[2] * 2)));
    return stats;
  }
}

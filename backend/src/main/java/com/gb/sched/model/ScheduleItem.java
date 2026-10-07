package com.gb.sched.model;

public record ScheduleItem(
    String date,
    String department,
    String position,
    String staffName,
    String shift,
    boolean holiday,
    String color,
    // 该格子是否被护士长手动改过（一键生成时会被保留）
    boolean adjusted,
    // 该格子上的冲突类型：none / nightToDay / continuousDays
    String conflict) {

  public static final String CONFLICT_NONE = "none";
  public static final String CONFLICT_NIGHT_TO_DAY = "nightToDay";
  public static final String CONFLICT_CONTINUOUS_DAYS = "continuousDays";

  public ScheduleItem withConflict(String newConflict) {
    return new ScheduleItem(date, department, position, staffName, shift, holiday, color, adjusted, newConflict);
  }

  public ScheduleItem withShift(String newShift, String newColor, boolean wasAdjusted) {
    return new ScheduleItem(date, department, position, staffName, newShift, holiday, newColor, wasAdjusted, CONFLICT_NONE);
  }
}

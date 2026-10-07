package com.gb.sched.model;

public record ScheduleItem(
    String date,
    String department,
    String position,
    String staffName,
    String shift,
    boolean holiday,
    String color,
    boolean manual,
    boolean overLimit) {

  public ScheduleItem(String date, String department, String position, String staffName, String shift, boolean holiday, String color) {
    this(date, department, position, staffName, shift, holiday, color, false, false);
  }

  public ScheduleItem withShift(String newShift, String newColor) {
    return new ScheduleItem(date, department, position, staffName, newShift, holiday, newColor, true, overLimit);
  }

  public ScheduleItem withOverLimit(boolean flag) {
    return new ScheduleItem(date, department, position, staffName, shift, holiday, color, manual, flag);
  }
}

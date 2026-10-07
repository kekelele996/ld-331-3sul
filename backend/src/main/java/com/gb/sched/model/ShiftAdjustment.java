package com.gb.sched.model;

/** 手动换班调整记录（留痕可追溯）。 */
public record ShiftAdjustment(
    Long id,
    String department,
    String date,
    String staffName,
    String oldShift,
    String newShift,
    String reason,
    String operator,
    String createdAt) {}

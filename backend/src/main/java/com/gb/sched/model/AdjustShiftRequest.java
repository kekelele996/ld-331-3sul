package com.gb.sched.model;

/** 护士长在排班表上直接换班的请求体。 */
public record AdjustShiftRequest(
    String department,
    String date,
    String staffName,
    String newShift,
    String reason) {}

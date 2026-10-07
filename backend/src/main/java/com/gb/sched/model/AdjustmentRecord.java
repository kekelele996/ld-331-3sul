package com.gb.sched.model;

public record AdjustmentRecord(
    int id,
    String date,
    String staffName,
    String oldShift,
    String newShift,
    String operator,
    String adjustedAt) {}

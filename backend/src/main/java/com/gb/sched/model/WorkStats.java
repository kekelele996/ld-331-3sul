package com.gb.sched.model;

public record WorkStats(
    String staffName,
    int dayShift,
    int middleShift,
    int nightShift,
    int restDays,
    // 实际上班天数（白班 + 中班 + 夜班，休息不计）
    int workDays,
    // 加班小时：夜班按 1.5 个班次折算
    int overtimeHours) {}

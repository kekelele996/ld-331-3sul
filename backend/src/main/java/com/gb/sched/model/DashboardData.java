package com.gb.sched.model;

import java.util.List;

/** /api/dashboard 及换班/重铺接口的统一返回体，各部分都来自同一份当前排班。 */
public record DashboardData(
    List<String> rules,
    String regeneratePolicy,
    List<ScheduleItem> schedule,
    List<ConflictAlert> conflicts,
    List<ShiftRequest> requests,
    List<ShiftAdjustment> adjustments,
    List<WorkStats> stats) {}

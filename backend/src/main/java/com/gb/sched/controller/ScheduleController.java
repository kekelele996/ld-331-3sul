package com.gb.sched.controller;

import com.gb.sched.config.AppConstants;
import com.gb.sched.model.AdjustShiftRequest;
import com.gb.sched.model.DashboardData;
import com.gb.sched.model.Department;
import com.gb.sched.model.ScheduleItem;
import com.gb.sched.service.DepartmentService;
import com.gb.sched.service.ScheduleRuleService;
import com.gb.sched.service.ScheduleStateService;
import com.gb.sched.service.ShiftRequestService;
import com.gb.sched.service.StatsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScheduleController {

  private static final List<String> RULES =
      List.of("连续工作不超过 5 天（休息不计入连续）", "周末轮循", "夜班后不接白班", "节假日按优先级排班");

  private final DepartmentService departmentService;
  private final ScheduleRuleService scheduleRuleService;
  private final ScheduleStateService scheduleStateService;
  private final ShiftRequestService shiftRequestService;
  private final StatsService statsService;

  public ScheduleController(
      DepartmentService departmentService,
      ScheduleRuleService scheduleRuleService,
      ScheduleStateService scheduleStateService,
      ShiftRequestService shiftRequestService,
      StatsService statsService) {
    this.departmentService = departmentService;
    this.scheduleRuleService = scheduleRuleService;
    this.scheduleStateService = scheduleStateService;
    this.shiftRequestService = shiftRequestService;
    this.statsService = statsService;
  }

  @GetMapping("/dashboard")
  public DashboardData dashboard(@RequestParam(name = "department", defaultValue = "急诊科") String department) {
    return buildDashboard(department);
  }

  /** 护士长直接在排班表上给某人换班次（白班/中班/夜班/休息）。 */
  @PostMapping("/schedule/adjust")
  public DashboardData adjustShift(@RequestBody AdjustShiftRequest request) {
    scheduleStateService.adjustShift(request);
    return buildDashboard(request.department());
  }

  /**
   * 一键生成：按「人 × 日期」保住调整记录里改过的格子，引擎只重铺没动过的人/格子。
   */
  @PostMapping("/schedule/regenerate")
  public DashboardData regenerate(@RequestParam(name = "department", defaultValue = "急诊科") String department) {
    scheduleStateService.regenerate(department);
    return buildDashboard(department);
  }

  @GetMapping("/departments")
  public List<Department> departments() {
    return departmentService.listDepartments();
  }

  private DashboardData buildDashboard(String department) {
    List<ScheduleItem> schedule = scheduleStateService.currentSchedule(department);
    return new DashboardData(
        RULES,
        AppConstants.REGENERATE_POLICY,
        schedule,
        scheduleRuleService.toAlerts(schedule),
        shiftRequestService.listRequests(),
        scheduleStateService.adjustments(department),
        statsService.monthlyStats(schedule));
  }
}

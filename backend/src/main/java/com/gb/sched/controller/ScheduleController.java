package com.gb.sched.controller;

import com.gb.sched.model.AdjustShiftRequest;
import com.gb.sched.model.Department;
import com.gb.sched.model.GenerateRequest;
import com.gb.sched.model.ScheduleItem;
import com.gb.sched.service.DepartmentService;
import com.gb.sched.service.ScheduleRuleService;
import com.gb.sched.service.ScheduleStore;
import com.gb.sched.service.ShiftRequestService;
import com.gb.sched.service.StatsService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ScheduleController {
  private final DepartmentService departmentService;
  private final ScheduleRuleService scheduleRuleService;
  private final ScheduleStore scheduleStore;
  private final ShiftRequestService shiftRequestService;
  private final StatsService statsService;

  public ScheduleController(DepartmentService departmentService, ScheduleRuleService scheduleRuleService, ScheduleStore scheduleStore, ShiftRequestService shiftRequestService, StatsService statsService) {
    this.departmentService = departmentService;
    this.scheduleRuleService = scheduleRuleService;
    this.scheduleStore = scheduleStore;
    this.shiftRequestService = shiftRequestService;
    this.statsService = statsService;
  }

  @GetMapping("/dashboard")
  public Map<String, Object> dashboard(@RequestParam(name = "department", defaultValue = "急诊科") String department) {
    return buildDashboard(department);
  }

  @PostMapping("/schedule/adjust")
  public Map<String, Object> adjust(@RequestBody AdjustShiftRequest request) {
    try {
      scheduleStore.adjust(request.department(), request.date(), request.staffName(), request.shift());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
    }
    return buildDashboard(request.department());
  }

  @PostMapping("/schedule/generate")
  public Map<String, Object> generate(@RequestBody GenerateRequest request) {
    scheduleStore.regenerate(request.department());
    return buildDashboard(request.department());
  }

  @GetMapping("/departments")
  public List<Department> departments() {
    return departmentService.listDepartments();
  }

  private Map<String, Object> buildDashboard(String department) {
    List<ScheduleItem> schedule = scheduleRuleService.markContinuousOverLimit(scheduleStore.scheduleFor(department));
    return Map.of(
        "rules", List.of("连续工作不超过 5 天", "周末轮循", "夜班后不接白班", "节假日按优先级排班"),
        "schedule", schedule,
        "conflicts", scheduleRuleService.detectConflicts(schedule),
        "requests", shiftRequestService.listRequests(),
        "stats", statsService.monthlyStats(schedule),
        "adjustments", scheduleStore.adjustmentsFor(department));
  }
}

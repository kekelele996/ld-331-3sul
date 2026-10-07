package com.gb.sched.config;

import java.util.List;
import java.util.Map;

public final class AppConstants {
  private AppConstants() {}

  public static final List<String> SHIFT_TYPES = List.of("白班", "中班", "夜班", "休息");
  public static final Map<String, String> SHIFT_COLORS = Map.of(
      "白班", "#409eff",
      "中班", "#67c23a",
      "夜班", "#626aef",
      "休息", "#909399");

  /** 连续上班上限：超过 5 天即冲突，休息不计入连续。 */
  public static final int MAX_CONTINUOUS_WORK_DAYS = 5;

  public static final List<String> ROLES = List.of("管理员", "主管", "医护人员");
  public static final String CONFLICT_NIGHT_TO_DAY = "夜班后不能直接接白班";
  public static final String CONFLICT_MAX_CONTINUOUS_DAYS = "连续工作天数超过 5 天上限（休息不计入连续）";

  /** 当前演示环境中执行手动调整的操作者。 */
  public static final String DEFAULT_OPERATOR = "护士长";

  /**
   * 一键生成策略（方案三）：按「人 × 日期」保住调整记录里改过的班次，
   * 引擎重铺只覆盖没动过的格子，保证同一天同一个人的班次与调整记录一致。
   */
  public static final String REGENERATE_POLICY =
      "一键生成按「人 × 日期」保留调整记录中手动改过的班次，引擎只重铺未调整的格子；调整记录持续留痕，同一天同一人的班次始终与调整记录一致。";
}

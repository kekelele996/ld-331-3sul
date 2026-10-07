export interface ScheduleItem {
  date: string;
  department: string;
  position: string;
  staffName: string;
  shift: string;
  holiday: boolean;
  color: string;
  /** 是否为护士长手动改过的格子 */
  adjusted: boolean;
  /** none / nightToDay / continuousDays */
  conflict: string;
}

export interface ConflictAlert {
  level: string;
  staffName: string;
  date: string;
  message: string;
}

export interface ShiftRequest {
  id: number;
  applicant: string;
  replacement: string;
  date: string;
  reason: string;
  status: string;
}

export interface ShiftAdjustment {
  id: number;
  department: string;
  date: string;
  staffName: string;
  oldShift: string;
  newShift: string;
  reason: string;
  operator: string;
  createdAt: string;
}

export interface WorkStats {
  staffName: string;
  dayShift: number;
  middleShift: number;
  nightShift: number;
  restDays: number;
  workDays: number;
  overtimeHours: number;
}

export interface DashboardData {
  rules: string[];
  regeneratePolicy: string;
  schedule: ScheduleItem[];
  conflicts: ConflictAlert[];
  requests: ShiftRequest[];
  adjustments: ShiftAdjustment[];
  stats: WorkStats[];
}

export interface AdjustShiftPayload {
  department: string;
  date: string;
  staffName: string;
  newShift: string;
  reason?: string;
}

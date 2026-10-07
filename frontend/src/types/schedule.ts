export interface ScheduleItem {
  date: string;
  department: string;
  position: string;
  staffName: string;
  shift: string;
  holiday: boolean;
  color: string;
  manual: boolean;
  overLimit: boolean;
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

export interface WorkStats {
  staffName: string;
  dayShift: number;
  middleShift: number;
  nightShift: number;
  restDays: number;
  overtimeHours: number;
}

export interface AdjustmentRecord {
  id: number;
  date: string;
  staffName: string;
  oldShift: string;
  newShift: string;
  operator: string;
  adjustedAt: string;
}

export interface DashboardData {
  rules: string[];
  schedule: ScheduleItem[];
  conflicts: ConflictAlert[];
  requests: ShiftRequest[];
  stats: WorkStats[];
  adjustments: AdjustmentRecord[];
}

import { apiClient } from './client';
import type { AdjustShiftPayload, DashboardData } from '../types/schedule';

export async function fetchDashboard(department: string) {
  const { data } = await apiClient.get<DashboardData>('/dashboard', { params: { department } });
  return data;
}

/** 护士长在排班表上直接给某人换班次。 */
export async function adjustShift(payload: AdjustShiftPayload) {
  const { data } = await apiClient.post<DashboardData>('/schedule/adjust', payload);
  return data;
}

/** 一键生成：保留手动改过的格子，重铺未调整的格子。 */
export async function regenerateSchedule(department: string) {
  const { data } = await apiClient.post<DashboardData>('/schedule/regenerate', null, {
    params: { department },
  });
  return data;
}

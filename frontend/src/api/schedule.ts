import { apiClient } from './client';
import type { DashboardData } from '../types/schedule';

export async function fetchDashboard(department: string) {
  const { data } = await apiClient.get<DashboardData>('/dashboard', { params: { department } });
  return data;
}

export async function adjustShift(payload: { department: string; date: string; staffName: string; shift: string }) {
  const { data } = await apiClient.post<DashboardData>('/schedule/adjust', payload);
  return data;
}

export async function regenerateSchedule(department: string) {
  const { data } = await apiClient.post<DashboardData>('/schedule/generate', { department });
  return data;
}

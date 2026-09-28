import type { ApiResponse } from '@/types/api'
import type { WeeklyDetail, WeeklyPayload } from '@/types/weekly'
import request from './request'

export function getWeekly(id: string) {
  return request.get<ApiResponse<WeeklyDetail>>(`/admin/api/weeklies/${id}`)
}

export function createWeekly(payload: WeeklyPayload) {
  return request.post<ApiResponse<WeeklyDetail>>('/admin/api/weeklies', payload)
}

export function updateWeekly(id: string, payload: WeeklyPayload) {
  return request.put<ApiResponse<WeeklyDetail>>(`/admin/api/weeklies/${id}`, payload)
}

export const weeklyApi = {
  getWeekly,
  createWeekly,
  updateWeekly,
}

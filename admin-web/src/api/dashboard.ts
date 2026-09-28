import type { ApiResponse } from '@/types/api'
import type { DashboardOverview } from '@/types/dashboard'
import request from './request'

export function getDashboardOverview() {
  return request.get<ApiResponse<DashboardOverview>>('/admin/api/dashboard/overview')
}

export const dashboardApi = {
  getOverview: getDashboardOverview,
}

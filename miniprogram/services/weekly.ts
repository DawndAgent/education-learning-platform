import { request } from './request'
import { WeeklyDetailVO } from '../types/content'

export function getWeeklyDetail(contentId: string): Promise<WeeklyDetailVO> {
  return request.get<WeeklyDetailVO>(`/api/weeklies/${encodeURIComponent(contentId)}`)
}

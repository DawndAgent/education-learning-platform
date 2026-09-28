import { request } from './request'
import { VideoDetailVO } from '../types/content'

export function getVideoDetail(contentId: string): Promise<VideoDetailVO> {
  return request.get<VideoDetailVO>(`/api/videos/${encodeURIComponent(contentId)}`)
}

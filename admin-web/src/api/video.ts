import type { ApiResponse } from '@/types/api'
import type { VideoDetail, VideoPayload } from '@/types/video'
import request from './request'

export function getVideo(id: string) {
  return request.get<ApiResponse<VideoDetail>>(`/admin/api/videos/${id}`)
}

export function createVideo(payload: VideoPayload) {
  return request.post<ApiResponse<VideoDetail>>('/admin/api/videos', payload)
}

export function updateVideo(id: string, payload: VideoPayload) {
  return request.put<ApiResponse<VideoDetail>>(`/admin/api/videos/${id}`, payload)
}

export const videoApi = {
  getVideo,
  createVideo,
  updateVideo,
}

import type { ApiResponse } from '@/types/api'
import type {
  Content,
  ContentBatchResult,
  ContentCreateRequest,
  ContentQuery,
  ContentUpdateRequest,
  PageResult,
} from '@/types/content'
import request from './request'

export function getContentList(query: ContentQuery) {
  return request.get<ApiResponse<PageResult<Content>>>('/admin/api/contents', {
    params: toListParams(query),
  })
}

export function getContentDetail(id: string) {
  return request.get<ApiResponse<Content>>(`/admin/api/contents/${id}`)
}

export function createContent(payload: ContentCreateRequest) {
  return request.post<ApiResponse<Content>>('/admin/api/contents', payload)
}

export function updateContent(id: string, payload: ContentUpdateRequest) {
  return request.put<ApiResponse<Content>>(`/admin/api/contents/${id}`, payload)
}

export function deleteContent(id: string) {
  return request.delete<ApiResponse<null>>(`/admin/api/contents/${id}`)
}

export function publishContent(id: string) {
  return request.post<ApiResponse<Content>>(`/admin/api/contents/${id}/publish`)
}

export function offlineContent(id: string) {
  return request.post<ApiResponse<Content>>(`/admin/api/contents/${id}/offline`)
}

export function duplicateContent(id: string) {
  return request.post<ApiResponse<Content>>(`/admin/api/contents/${id}/duplicate`)
}

export function batchOfflineContents(ids: string[]) {
  return request.post<ApiResponse<ContentBatchResult>>('/admin/api/contents/batch-offline', { ids })
}

export function batchDeleteContents(ids: string[]) {
  return request.post<ApiResponse<ContentBatchResult>>('/admin/api/contents/batch-delete', { ids })
}

export function batchPublishContents(ids: string[]) {
  return request.post<ApiResponse<ContentBatchResult>>('/admin/api/contents/batch-publish', { ids })
}

export function schedulePublish(id: string, publishTime: string) {
  return request.post<ApiResponse<Content>>(`/admin/api/contents/${id}/schedule-publish`, { publishTime })
}

export function cancelScheduledPublish(id: string) {
  return request.post<ApiResponse<Content>>(`/admin/api/contents/${id}/cancel-scheduled-publish`)
}

export const contentApi = {
  getContentList,
  getContentDetail,
  createContent,
  updateContent,
  deleteContent,
  publishContent,
  offlineContent,
  duplicateContent,
  batchOfflineContents,
  batchDeleteContents,
  batchPublishContents,
  schedulePublish,
  cancelScheduledPublish,
}

function toListParams(query: ContentQuery): Record<string, string | number> {
  const params: Record<string, string | number> = {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
  }
  const keyword = query.keyword.trim()
  if (keyword) {
    params.keyword = keyword
  }
  if (query.categoryId) {
    params.categoryId = query.categoryId
  }
  if (query.contentType) {
    params.contentType = query.contentType
  }
  if (query.status) {
    params.status = query.status
  }
  if (query.schedule) {
    params.schedule = query.schedule
  }
  if (query.publishTimeFrom) {
    params.publishTimeFrom = query.publishTimeFrom
  }
  if (query.publishTimeTo) {
    params.publishTimeTo = query.publishTimeTo
  }
  return params
}

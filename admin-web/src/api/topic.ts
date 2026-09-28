import type { ApiResponse } from '@/types/api'
import type { PageResult } from '@/types/content'
import type {
  Topic,
  TopicContentAddRequest,
  TopicContentItem,
  TopicContentSortRequest,
  TopicCreateRequest,
  TopicItemBatchResult,
  TopicQuery,
  TopicUpdateRequest,
} from '@/types/topic'
import request from './request'

export function getTopicList(query: TopicQuery) {
  return request.get<ApiResponse<PageResult<Topic>>>('/admin/api/topics', {
    params: toListParams(query),
  })
}

export function getTopicDetail(id: string) {
  return request.get<ApiResponse<Topic>>(`/admin/api/topics/${id}`)
}

export function createTopic(payload: TopicCreateRequest) {
  return request.post<ApiResponse<Topic>>('/admin/api/topics', payload)
}

export function updateTopic(id: string, payload: TopicUpdateRequest) {
  return request.put<ApiResponse<Topic>>(`/admin/api/topics/${id}`, payload)
}

export function deleteTopic(id: string) {
  return request.delete<ApiResponse<null>>(`/admin/api/topics/${id}`)
}

export function publishTopic(id: string) {
  return request.post<ApiResponse<Topic>>(`/admin/api/topics/${id}/publish`)
}

export function offlineTopic(id: string) {
  return request.post<ApiResponse<Topic>>(`/admin/api/topics/${id}/offline`)
}

export function getTopicContents(topicId: string) {
  return request.get<ApiResponse<TopicContentItem[]>>(`/admin/api/topics/${topicId}/contents`)
}

export function addTopicItems(topicId: string, contentIds: string[]) {
  return request.post<ApiResponse<TopicItemBatchResult>>(`/admin/api/topics/${topicId}/items/batch`, {
    contentIds,
  })
}

export function addTopicContents(topicId: string, payload: TopicContentAddRequest) {
  return request.post<ApiResponse<TopicContentItem[]>>(`/admin/api/topics/${topicId}/contents`, payload)
}

export function removeTopicContent(topicId: string, contentId: string) {
  return request.delete<ApiResponse<null>>(`/admin/api/topics/${topicId}/contents/${contentId}`)
}

export function sortTopicContents(topicId: string, payload: TopicContentSortRequest) {
  return request.put<ApiResponse<TopicContentItem[]>>(`/admin/api/topics/${topicId}/contents/sort`, payload)
}

export const topicApi = {
  getTopicList,
  getTopicDetail,
  createTopic,
  updateTopic,
  deleteTopic,
  publishTopic,
  offlineTopic,
  getTopicContents,
  addTopicContents,
  addTopicItems,
  removeTopicContent,
  sortTopicContents,
}

function toListParams(query: TopicQuery): Record<string, string | number> {
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
  if (query.status) {
    params.status = query.status
  }
  return params
}

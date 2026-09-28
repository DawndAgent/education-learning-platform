import { request } from './request'
import type { TopicDetail, TopicListItem, TopicPage, TopicQuery } from '../types/topic'

export function getTopicList(params: TopicQuery): Promise<TopicPage> {
  return request.get<TopicPage>('/api/topics', params)
}

export function getFeaturedTopics(pageSize = 4): Promise<TopicListItem[]> {
  return request.get<TopicListItem[]>('/api/topics/featured', { pageSize })
}

export function getTopicDetail(id: string): Promise<TopicDetail> {
  return request.get<TopicDetail>(`/api/topics/${encodeURIComponent(id)}`)
}

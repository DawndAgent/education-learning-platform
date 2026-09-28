import { request } from './request'
import { ContentDetailVO, ContentPage, ContentQuery } from '../types/content'

export function getContentList(params: ContentQuery): Promise<ContentPage> {
  return request.get<ContentPage>('/api/content', params)
}

export function getContentDetail(id: string): Promise<ContentDetailVO> {
  return request.get<ContentDetailVO>(`/api/content/${encodeURIComponent(id)}`)
}

export function recordContentView(id: string): Promise<null> {
  return request.post<null>(`/api/content/${encodeURIComponent(id)}/view`)
}

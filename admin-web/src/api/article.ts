import type { ApiResponse } from '@/types/api'
import type { ArticleDetail, ArticlePayload } from '@/types/article'
import request from './request'

export function getArticle(id: string) {
  return request.get<ApiResponse<ArticleDetail>>(`/admin/api/articles/${id}`)
}

export function createArticle(payload: ArticlePayload) {
  return request.post<ApiResponse<ArticleDetail>>('/admin/api/articles', payload)
}

export function updateArticle(id: string, payload: ArticlePayload) {
  return request.put<ApiResponse<ArticleDetail>>(`/admin/api/articles/${id}`, payload)
}

export const articleApi = {
  getArticle,
  createArticle,
  updateArticle,
}

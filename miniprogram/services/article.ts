import { request } from './request'
import { ArticleDetailVO } from '../types/content'

export function getArticleDetail(contentId: string): Promise<ArticleDetailVO> {
  return request.get<ArticleDetailVO>(`/api/articles/${encodeURIComponent(contentId)}`)
}

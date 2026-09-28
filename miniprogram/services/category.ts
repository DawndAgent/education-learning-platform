import { request } from './request'
import { CategoryTreeVO, CategoryVO } from '../types/category'

export function getCategoryTree(): Promise<CategoryTreeVO[]> {
  return request.get<CategoryTreeVO[]>('/api/categories/tree')
}

export function getCategoryDetail(id: string): Promise<CategoryVO> {
  return request.get<CategoryVO>(`/api/categories/${encodeURIComponent(id)}`)
}

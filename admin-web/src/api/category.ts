import type { ApiResponse } from '@/types/api'
import type { Category, CategoryPayload } from '@/types/category'
import request from './request'

export function getCategoryTree() {
  return request.get<ApiResponse<Category[]>>('/admin/api/categories/tree')
}

export function createCategory(payload: CategoryPayload) {
  return request.post<ApiResponse<Category>>('/admin/api/categories', payload)
}

export function updateCategory(id: string, payload: CategoryPayload) {
  return request.put<ApiResponse<Category>>(`/admin/api/categories/${id}`, payload)
}

export function deleteCategory(id: string) {
  return request.delete<ApiResponse<null>>(`/admin/api/categories/${id}`)
}

export const categoryApi = {
  getCategoryTree,
  createCategory,
  updateCategory,
  deleteCategory,
}

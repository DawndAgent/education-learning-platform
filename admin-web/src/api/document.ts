import type { ApiResponse } from '@/types/api'
import type { DocumentDetail, DocumentPayload } from '@/types/document'
import request from './request'

export function getDocument(id: string) {
  return request.get<ApiResponse<DocumentDetail>>(`/admin/api/documents/${id}`)
}

export function createDocument(payload: DocumentPayload) {
  return request.post<ApiResponse<DocumentDetail>>('/admin/api/documents', payload)
}

export function updateDocument(id: string, payload: DocumentPayload) {
  return request.put<ApiResponse<DocumentDetail>>(`/admin/api/documents/${id}`, payload)
}

export const documentApi = {
  getDocument,
  createDocument,
  updateDocument,
}

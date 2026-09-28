import { request } from './request'
import { DocumentDetailVO } from '../types/content'

export function getDocumentDetail(contentId: string): Promise<DocumentDetailVO> {
  return request.get<DocumentDetailVO>(`/api/documents/${encodeURIComponent(contentId)}`)
}

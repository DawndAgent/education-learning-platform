import type { ApiResponse } from '@/types/api'
import type { QuestionDetail, QuestionPayload } from '@/types/question'
import request from './request'

export function getQuestion(id: string) {
  return request.get<ApiResponse<QuestionDetail>>(`/admin/api/questions/${id}`)
}

export function createQuestion(payload: QuestionPayload) {
  return request.post<ApiResponse<QuestionDetail>>('/admin/api/questions', payload)
}

export function updateQuestion(id: string, payload: QuestionPayload) {
  return request.put<ApiResponse<QuestionDetail>>(`/admin/api/questions/${id}`, payload)
}

export const questionApi = {
  getQuestion,
  createQuestion,
  updateQuestion,
}

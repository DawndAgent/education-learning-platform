import type { ApiResponse } from '@/types/api'
import type { QuestionBatchCreateResult, QuestionPdfParseResult } from '@/types/question-import'
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

export function parseQuestionPdf(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<ApiResponse<QuestionPdfParseResult>>('/admin/api/questions/import/parse', form, {
    timeout: 120000,
  })
}

export function batchCreateQuestions(items: QuestionPayload[]) {
  return request.post<ApiResponse<QuestionBatchCreateResult>>('/admin/api/questions/import/batch', {
    items,
  })
}

export const questionApi = {
  getQuestion,
  createQuestion,
  updateQuestion,
  parseQuestionPdf,
  batchCreateQuestions,
}

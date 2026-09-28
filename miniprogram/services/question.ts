import { request } from './request'
import { QuestionDetailVO } from '../types/content'

export function getQuestionDetail(contentId: string): Promise<QuestionDetailVO> {
  return request.get<QuestionDetailVO>(`/api/questions/${encodeURIComponent(contentId)}`)
}

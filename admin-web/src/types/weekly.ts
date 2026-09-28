export interface WeeklyDetail {
  contentId: string
  title: string
  categoryId: string
  coverUrl: string | null
  summary: string | null
  status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
  sort: number
  publishTime: string | null
  scheduledPublishTime: string | null
  weekLabel: string | null
  questionText: string | null
  questionImageUrl: string | null
  answerText: string | null
  answerImageUrl: string | null
  analysisText: string | null
  analysisImageUrl: string | null
}

export interface WeeklyPayload {
  categoryId: string
  title: string
  coverUrl: string
  summary: string
  sort: number
  weekLabel: string
  questionText: string
  questionImageUrl: string
  answerText: string
  answerImageUrl: string
  analysisText: string
  analysisImageUrl: string
}

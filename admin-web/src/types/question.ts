export type QuestionType =
  | 'SINGLE_CHOICE'
  | 'MULTIPLE_CHOICE'
  | 'FILL_BLANK'
  | 'ANSWER'
  | 'PROOF'
  | 'IMAGE_QUESTION'

export type QuestionDifficulty = 'EASY' | 'MEDIUM' | 'HARD'

export interface QuestionDetail {
  contentId: string
  title: string
  categoryId: string
  coverUrl: string | null
  summary: string | null
  status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
  sort: number
  publishTime: string | null
  scheduledPublishTime: string | null
  questionType: QuestionType | null
  questionText: string | null
  questionImageUrl: string | null
  answerText: string | null
  answerImageUrl: string | null
  analysisText: string | null
  analysisImageUrl: string | null
  difficulty: QuestionDifficulty | null
}

export interface QuestionPayload {
  categoryId: string
  title: string
  coverUrl: string
  summary: string
  sort: number
  questionType: QuestionType
  questionText: string
  questionImageUrl: string
  answerText: string
  answerImageUrl: string
  analysisText: string
  analysisImageUrl: string
  difficulty: QuestionDifficulty | null
}

import type { QuestionDifficulty, QuestionType } from './question'

export interface QuestionPdfParsedItem {
  index: number
  label: string
  title: string
  questionType: QuestionType
  questionText: string
  answerText: string
  analysisText: string
  suspicious: boolean
  suspiciousReason: string | null
}

export interface QuestionPdfParseResult {
  fileName: string
  pageCount: number
  questionCount: number
  questions: QuestionPdfParsedItem[]
}

export interface QuestionImportDraft {
  key: string
  selected: boolean
  title: string
  questionType: QuestionType
  questionText: string
  answerText: string
  analysisText: string
  difficulty: QuestionDifficulty | ''
  suspicious: boolean
  suspiciousReason: string | null
  label: string
}

export interface QuestionBatchCreateResult {
  createdCount: number
  contentIds: string[]
}

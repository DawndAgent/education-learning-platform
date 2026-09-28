import type { QuestionDifficulty, QuestionPayload, QuestionType } from '../types/question'

export interface QuestionFormValues {
  title: string
  categoryId: string
  coverUrl: string
  summary: string
  sort: number | null
  questionType: QuestionType | ''
  questionText: string
  questionImageUrl: string
  answerText: string
  answerImageUrl: string
  analysisText: string
  analysisImageUrl: string
  difficulty: QuestionDifficulty | ''
}

const QUESTION_TYPES: readonly QuestionType[] = [
  'SINGLE_CHOICE',
  'MULTIPLE_CHOICE',
  'FILL_BLANK',
  'ANSWER',
  'PROOF',
  'IMAGE_QUESTION',
]

export function questionTypeLabel(type: QuestionType): string {
  const labels: Record<QuestionType, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    FILL_BLANK: '填空题',
    ANSWER: '解答题',
    PROOF: '证明题',
    IMAGE_QUESTION: '看图题',
  }
  return labels[type]
}

export function difficultyLabel(difficulty: QuestionDifficulty): string {
  if (difficulty === 'EASY') {
    return '简单'
  }
  if (difficulty === 'MEDIUM') {
    return '中等'
  }
  return '困难'
}

export function validateQuestionForm(form: QuestionFormValues, leaves: readonly string[]): string | null {
  const title = form.title.trim()
  if (!title) {
    return '标题不能为空'
  }
  if (title.length > 128) {
    return '标题长度不能超过128'
  }
  if (!form.categoryId || !leaves.includes(form.categoryId)) {
    return '内容只能选择二级分类'
  }
  if (form.sort === null || !Number.isInteger(form.sort) || form.sort < 0 || form.sort > 9999) {
    return '排序范围为 0 到 9999'
  }
  if (!QUESTION_TYPES.includes(form.questionType as QuestionType)) {
    return '请选择题型'
  }
  if (form.questionText.length > 20000) {
    return '题目内容长度不能超过20000'
  }
  if (form.answerText.length > 20000) {
    return '答案长度不能超过20000'
  }
  if (form.analysisText.length > 20000) {
    return '解析长度不能超过20000'
  }
  if (form.questionImageUrl.length > 500) {
    return '题目图片地址长度不能超过500'
  }
  if (form.answerImageUrl.length > 500) {
    return '答案图片地址长度不能超过500'
  }
  if (form.analysisImageUrl.length > 500) {
    return '解析图片地址长度不能超过500'
  }
  if (form.coverUrl.length > 255) {
    return '封面地址长度不能超过255'
  }
  if (form.summary.length > 512) {
    return '摘要长度不能超过512'
  }
  if (form.difficulty && form.difficulty !== 'EASY' && form.difficulty !== 'MEDIUM' && form.difficulty !== 'HARD') {
    return '难度不合法'
  }
  return null
}

export function toQuestionPayload(form: QuestionFormValues): QuestionPayload {
  return {
    title: form.title.trim(),
    categoryId: form.categoryId,
    coverUrl: form.coverUrl.trim(),
    summary: form.summary.trim(),
    sort: form.sort ?? 0,
    questionType: form.questionType as QuestionType,
    questionText: form.questionText.trim(),
    questionImageUrl: form.questionImageUrl.trim(),
    answerText: form.answerText.trim(),
    answerImageUrl: form.answerImageUrl.trim(),
    analysisText: form.analysisText.trim(),
    analysisImageUrl: form.analysisImageUrl.trim(),
    difficulty: form.difficulty || null,
  }
}

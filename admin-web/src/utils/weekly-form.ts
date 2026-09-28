import type { WeeklyPayload } from '../types/weekly'

export interface WeeklyFormValues {
  title: string
  categoryId: string
  coverUrl: string
  summary: string
  sort: number | null
  weekLabel: string
  questionText: string
  questionImageUrl: string
  answerText: string
  answerImageUrl: string
  analysisText: string
  analysisImageUrl: string
}

export function validateWeeklyForm(form: WeeklyFormValues, leaves: readonly string[]): string | null {
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
  if (form.weekLabel.length > 50) {
    return '周次标签长度不能超过50'
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
  return null
}

export function toWeeklyPayload(form: WeeklyFormValues): WeeklyPayload {
  return {
    title: form.title.trim(),
    categoryId: form.categoryId,
    coverUrl: form.coverUrl.trim(),
    summary: form.summary.trim(),
    sort: form.sort ?? 0,
    weekLabel: form.weekLabel.trim(),
    questionText: form.questionText.trim(),
    questionImageUrl: form.questionImageUrl.trim(),
    answerText: form.answerText.trim(),
    answerImageUrl: form.answerImageUrl.trim(),
    analysisText: form.analysisText.trim(),
    analysisImageUrl: form.analysisImageUrl.trim(),
  }
}

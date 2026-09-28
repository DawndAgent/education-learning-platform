import type { DocumentPayload } from '../types/document'

export interface DocumentFormValues {
  title: string
  categoryId: string
  coverUrl: string
  summary: string
  sort: number | null
  fileUrl: string
  fileName: string
  fileSize: number | null
  fileType: string
  downloadUrl: string
  previewUrl: string
  description: string
}

export function validateDocumentForm(form: DocumentFormValues, leaves: readonly string[]): string | null {
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
  if (form.fileUrl.length > 1000) {
    return '文件地址长度不能超过1000'
  }
  if (form.fileName.length > 255) {
    return '文件名长度不能超过255'
  }
  if (form.fileType.length > 100) {
    return '文件类型长度不能超过100'
  }
  if (form.downloadUrl.length > 1000) {
    return '下载地址长度不能超过1000'
  }
  if (form.previewUrl.length > 1000) {
    return '预览地址长度不能超过1000'
  }
  if (form.description.length > 1000) {
    return '描述长度不能超过1000'
  }
  if (form.fileSize !== null && (!Number.isInteger(form.fileSize) || form.fileSize < 0)) {
    return '文件大小不能小于0'
  }
  if (form.coverUrl.length > 255) {
    return '封面地址长度不能超过255'
  }
  if (form.summary.length > 512) {
    return '摘要长度不能超过512'
  }
  return null
}

export function toDocumentPayload(form: DocumentFormValues): DocumentPayload {
  return {
    title: form.title.trim(),
    categoryId: form.categoryId,
    coverUrl: form.coverUrl.trim(),
    summary: form.summary.trim(),
    sort: form.sort ?? 0,
    fileUrl: form.fileUrl.trim(),
    fileName: form.fileName.trim(),
    fileSize: form.fileSize,
    fileType: form.fileType.trim(),
    downloadUrl: form.downloadUrl.trim(),
    previewUrl: form.previewUrl.trim(),
    description: form.description.trim(),
  }
}

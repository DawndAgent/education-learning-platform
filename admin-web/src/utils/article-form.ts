import type { ArticlePayload } from '../types/article'

export interface ArticleFormValues {
  title: string
  categoryId: string
  coverUrl: string
  summary: string
  sort: number | null
  body: string
  author: string
  source: string
}

export function previewHtml(html: string): string {
  return html
    .replace(/<\s*(script|iframe|object|embed)\b[^>]*>[\s\S]*?<\s*\/\s*\1\s*>/gi, '')
    .replace(/<\s*(script|iframe|object|embed)\b[^>]*\/?>/gi, '')
    .replace(/\son\w+\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)/gi, '')
    .replace(/javascript\s*:/gi, '')
}

export function articleText(html: string): string {
  return html
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

export function validateArticleForm(form: ArticleFormValues, leaves: readonly string[]): string | null {
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
  if (!articleText(form.body)) {
    return '正文不能为空'
  }
  if (form.body.length > 20000) {
    return '正文长度不能超过20000'
  }
  if (form.author.length > 64) {
    return '作者长度不能超过64'
  }
  if (form.source.length > 128) {
    return '来源长度不能超过128'
  }
  if (form.coverUrl.length > 255) {
    return '封面地址长度不能超过255'
  }
  if (form.summary.length > 512) {
    return '摘要长度不能超过512'
  }
  return null
}

export function toArticlePayload(form: ArticleFormValues): ArticlePayload {
  return {
    title: form.title.trim(),
    categoryId: form.categoryId,
    coverUrl: form.coverUrl.trim(),
    summary: form.summary.trim(),
    sort: form.sort ?? 0,
    body: form.body,
    author: form.author.trim(),
    source: form.source.trim(),
  }
}

/**
 * 插入到正文：小程序码图 + 说明。
 * 默认宽 220px；点选后拖四角改大小，拖图片本体可自由移动（margin），也可用工具栏对齐。
 */
export function buildVideoMiniprogramQrHtml(url: string, title: string, contentId: string): string {
  const safeUrl = url.trim().replace(/"/g, '&quot;')
  const safeTitle = title
    .trim()
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
  const label = safeTitle || '视频'
  const idAttr = String(contentId).replace(/"/g, '')
  return (
    `<p class="video-miniprogram-qr" data-video-id="${idAttr}">`
    + `<img src="${safeUrl}" alt="扫码观看视频：${label}" data-href="${safeUrl}" style="width:220px;" />`
    + `</p><p><span>扫码观看：${label}</span></p>`
  )
}

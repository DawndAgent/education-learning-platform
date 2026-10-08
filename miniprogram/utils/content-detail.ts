import type {
  ArticleDetailVO,
  ContentDetailVO,
  DocumentDetailVO,
  QuestionDetailVO,
  VideoDetailVO,
  WeeklyDetailVO
} from '../types/content'
import type { CategoryVO } from '../types/category'
import { ApiError, toErrorMessage } from './error'
import { formatPublishDate } from './content-view'
import { resolveMediaUrl } from './media-url'

export type DetailStatus = 'loading' | 'success' | 'error' | 'unsupported'
export type DetailKind = '' | 'article' | 'video' | 'question' | 'weekly' | 'document'

export interface DetailDeps {
  getContentDetail(id: string): Promise<ContentDetailVO>
  getArticleDetail(contentId: string): Promise<ArticleDetailVO>
  getVideoDetail(contentId: string): Promise<VideoDetailVO>
  getQuestionDetail(contentId: string): Promise<QuestionDetailVO>
  getWeeklyDetail(contentId: string): Promise<WeeklyDetailVO>
  getDocumentDetail(contentId: string): Promise<DocumentDetailVO>
  getCategoryDetail?(id: string): Promise<CategoryVO>
}

export interface DetailView {
  status: DetailStatus
  message: string
  canRetry: boolean
  kind: DetailKind
  title: string
  author: string
  source: string
  dateText: string
  coverUrl: string
  summary: string
  bodyHtml: string
  sourceLabel: string
  qrCodeUrl: string
  playUrl: string
  watchHint: string
  durationText: string
  categoryText: string
  weekLabel: string
  questionText: string
  questionImageUrl: string
  answerText: string
  answerImageUrl: string
  analysisText: string
  analysisImageUrl: string
  hasAnswer: boolean
  hasAnalysis: boolean
  fileName: string
  fileSizeText: string
  fileType: string
  description: string
  previewUrl: string
  downloadUrl: string
  fileUrl: string
}

const SOURCE_LABELS: Record<string, string> = {
  LOCAL: '平台内播放',
  WECHAT_CHANNEL: '微信视频号观看',
  TENCENT_VIDEO: '腾讯视频观看'
}

export function resolveContentId(rawId: string): { ok: true; id: string } | { ok: false; message: string } {
  const id = rawId.trim()
  if (!id) {
    return { ok: false, message: '内容参数缺失' }
  }
  if (!/^\d{1,20}$/.test(id)) {
    return { ok: false, message: '内容参数无效' }
  }
  return { ok: true, id }
}

export function contentFailureMessage(error: unknown): string {
  if (isNotFound(error)) {
    return '内容不存在或已下线'
  }
  return toErrorMessage(error, '内容加载失败，请稍后重试')
}

export function articleFailureMessage(error: unknown): string {
  if (isNotFound(error)) {
    return '内容不存在或已下线'
  }
  return toErrorMessage(error, '内容加载失败，请稍后重试')
}

export function videoFailureMessage(error: unknown): string {
  if (isNotFound(error)) {
    return '内容不存在或已下线'
  }
  return toErrorMessage(error, '内容加载失败，请稍后重试')
}

export function questionFailureMessage(error: unknown): string {
  if (isNotFound(error)) {
    return '内容不存在或已下线'
  }
  return toErrorMessage(error, '内容加载失败，请稍后重试')
}

export function weeklyFailureMessage(error: unknown): string {
  if (isNotFound(error)) {
    return '内容不存在或已下线'
  }
  return toErrorMessage(error, '内容加载失败，请稍后重试')
}

export function documentFailureMessage(error: unknown): string {
  if (isNotFound(error)) {
    return '内容不存在或已下线'
  }
  return toErrorMessage(error, '内容加载失败，请稍后重试')
}

export function formatDuration(seconds: number | null | undefined): string {
  if (seconds === null || seconds === undefined || !Number.isFinite(seconds) || seconds < 0) {
    return ''
  }
  const total = Math.floor(seconds)
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const remain = total % 60
  if (hours > 0) {
    return `${pad(hours)}:${pad(minutes)}:${pad(remain)}`
  }
  return `${pad(minutes)}:${pad(remain)}`
}

export function formatFileSize(bytes: string | number | null | undefined): string {
  if (bytes === null || bytes === undefined || bytes === '') {
    return ''
  }
  const value = typeof bytes === 'number' ? bytes : Number(bytes)
  if (!Number.isFinite(value) || value < 0) {
    return ''
  }
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }
  if (value < 1024 * 1024 * 1024) {
    return `${(value / (1024 * 1024)).toFixed(1)} MB`
  }
  return `${(value / (1024 * 1024 * 1024)).toFixed(1)} GB`
}

export function sourceLabel(sourceType: string): string {
  return SOURCE_LABELS[sourceType] || '暂不支持的播放来源'
}

export function prepareArticleHtml(body: string | null | undefined): string {
  const source = body || ''
  const html = looksLikeHtml(source) ? source : escapeText(source).replace(/\n/g, '<br/>')
  return sanitizeHtml(html)
}

export function isHttpUrl(url: string): boolean {
  return /^https?:\/\//i.test(url.trim())
}

export function blankDetail(): DetailView {
  return {
    status: 'loading',
    message: '',
    canRetry: false,
    kind: '',
    title: '',
    author: '',
    source: '',
    dateText: '',
    coverUrl: '',
    summary: '',
    bodyHtml: '',
    sourceLabel: '',
    qrCodeUrl: '',
    playUrl: '',
    watchHint: '',
    durationText: '',
    categoryText: '',
    weekLabel: '',
    questionText: '',
    questionImageUrl: '',
    answerText: '',
    answerImageUrl: '',
    analysisText: '',
    analysisImageUrl: '',
    hasAnswer: false,
    hasAnalysis: false,
    fileName: '',
    fileSizeText: '',
    fileType: '',
    description: '',
    previewUrl: '',
    downloadUrl: '',
    fileUrl: ''
  }
}

export function invalidDetail(message: string): DetailView {
  return {
    ...blankDetail(),
    status: 'error',
    message,
    canRetry: false
  }
}

export async function loadContentDetail(rawId: string, deps: DetailDeps): Promise<DetailView> {
  const param = resolveContentId(rawId)
  if (!param.ok) {
    return invalidDetail(param.message)
  }
  let content: ContentDetailVO
  try {
    content = await deps.getContentDetail(param.id)
  } catch (error) {
    return errorDetail(contentFailureMessage(error))
  }
  if (content.contentType === 'ARTICLE') {
    try {
      const article = await deps.getArticleDetail(param.id)
      return presentArticle(article)
    } catch (error) {
      return errorDetail(articleFailureMessage(error))
    }
  }
  if (content.contentType === 'VIDEO') {
    try {
      const video = await deps.getVideoDetail(param.id)
      return presentVideo(content, video)
    } catch (error) {
      return errorDetail(videoFailureMessage(error))
    }
  }
  if (content.contentType === 'QUESTION') {
    try {
      const question = await deps.getQuestionDetail(param.id)
      const categoryText = await resolveCategoryName(deps, question.categoryId)
      return presentQuestion(question, categoryText)
    } catch (error) {
      return errorDetail(questionFailureMessage(error))
    }
  }
  if (content.contentType === 'WEEKLY') {
    try {
      const weekly = await deps.getWeeklyDetail(param.id)
      const categoryText = await resolveCategoryName(deps, weekly.categoryId)
      return presentWeekly(weekly, categoryText)
    } catch (error) {
      return errorDetail(weeklyFailureMessage(error))
    }
  }
  if (content.contentType === 'DOCUMENT') {
    try {
      const document = await deps.getDocumentDetail(param.id)
      const categoryText = await resolveCategoryName(deps, document.categoryId)
      return presentDocument(document, categoryText)
    } catch (error) {
      return errorDetail(documentFailureMessage(error))
    }
  }
  return {
    ...blankDetail(),
    status: 'unsupported',
    message: '该内容类型暂不支持',
    title: content.title || ''
  }
}

function presentArticle(article: ArticleDetailVO): DetailView {
  return {
    ...blankDetail(),
    status: 'success',
    kind: 'article',
    title: article.title,
    author: article.author || '',
    source: article.source || '',
    dateText: formatPublishDate(article.publishTime),
    coverUrl: resolveMediaUrl(article.coverUrl || ''),
    summary: article.summary || '',
    bodyHtml: prepareArticleHtml(article.body)
  }
}

function presentVideo(content: ContentDetailVO, video: VideoDetailVO): DetailView {
  const qrCodeUrl = resolveMediaUrl(video.qrCodeUrl || '')
  const playUrl = video.sourceType === 'LOCAL' ? resolveMediaUrl(video.videoUrl || '') : ''
  return {
    ...blankDetail(),
    status: 'success',
    kind: 'video',
    title: video.title || content.title,
    dateText: formatPublishDate(video.publishTime || content.publishTime),
    coverUrl: resolveMediaUrl(video.coverUrl || content.coverUrl || ''),
    summary: video.summary || content.summary || '',
    sourceLabel: sourceLabel(video.sourceType),
    qrCodeUrl,
    playUrl,
    watchHint: playUrl ? '' : (qrCodeUrl ? '扫码观看完整视频' : '请扫码观看完整视频'),
    durationText: formatDuration(video.duration)
  }
}

function presentQuestion(question: QuestionDetailVO, categoryText: string): DetailView {
  const answerText = question.answerText || ''
  const answerImageUrl = question.answerImageUrl || ''
  const analysisText = question.analysisText || ''
  const analysisImageUrl = question.analysisImageUrl || ''
  return {
    ...blankDetail(),
    status: 'success',
    kind: 'question',
    title: question.title,
    categoryText,
    dateText: formatPublishDate(question.publishTime),
    coverUrl: resolveMediaUrl(question.coverUrl || ''),
    summary: question.summary || '',
    questionText: question.questionText || '',
    questionImageUrl: resolveMediaUrl(question.questionImageUrl || ''),
    answerText,
    answerImageUrl: resolveMediaUrl(answerImageUrl),
    analysisText,
    analysisImageUrl: resolveMediaUrl(analysisImageUrl),
    hasAnswer: Boolean(answerText || answerImageUrl),
    hasAnalysis: Boolean(analysisText || analysisImageUrl)
  }
}

function presentWeekly(weekly: WeeklyDetailVO, categoryText: string): DetailView {
  const answerText = weekly.answerText || ''
  const answerImageUrl = weekly.answerImageUrl || ''
  const analysisText = weekly.analysisText || ''
  const analysisImageUrl = weekly.analysisImageUrl || ''
  return {
    ...blankDetail(),
    status: 'success',
    kind: 'weekly',
    title: weekly.title,
    categoryText,
    weekLabel: weekly.weekLabel || '',
    dateText: formatPublishDate(weekly.publishTime),
    coverUrl: resolveMediaUrl(weekly.coverUrl || ''),
    summary: weekly.summary || '',
    questionText: weekly.questionText || '',
    questionImageUrl: resolveMediaUrl(weekly.questionImageUrl || ''),
    answerText,
    answerImageUrl: resolveMediaUrl(answerImageUrl),
    analysisText,
    analysisImageUrl: resolveMediaUrl(analysisImageUrl),
    hasAnswer: Boolean(answerText || answerImageUrl),
    hasAnalysis: Boolean(analysisText || analysisImageUrl)
  }
}

function presentDocument(document: DocumentDetailVO, categoryText: string): DetailView {
  return {
    ...blankDetail(),
    status: 'success',
    kind: 'document',
    title: document.title,
    categoryText,
    dateText: formatPublishDate(document.publishTime),
    coverUrl: resolveMediaUrl(document.coverUrl || ''),
    summary: document.summary || '',
    fileName: document.fileName || '',
    fileSizeText: formatFileSize(document.fileSize),
    fileType: document.fileType || '',
    description: document.description || '',
    previewUrl: resolveMediaUrl(document.previewUrl || ''),
    downloadUrl: resolveMediaUrl(document.downloadUrl || ''),
    fileUrl: resolveMediaUrl(document.fileUrl || '')
  }
}

async function resolveCategoryName(deps: DetailDeps, categoryId: string | null | undefined): Promise<string> {
  const id = (categoryId || '').trim()
  if (!id || !deps.getCategoryDetail) {
    return ''
  }
  try {
    const category = await deps.getCategoryDetail(id)
    return category.name || ''
  } catch {
    return ''
  }
}

function errorDetail(message: string): DetailView {
  return {
    ...blankDetail(),
    status: 'error',
    message,
    canRetry: true
  }
}

function isNotFound(error: unknown): boolean {
  return error instanceof ApiError && (error.code === '404' || error.httpStatus === 404)
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function looksLikeHtml(value: string): boolean {
  return /<\/?[a-z][\s\S]*>/i.test(value)
}

function escapeText(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function sanitizeHtml(html: string): string {
  const withoutBlocked = html
    .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, '')
    .replace(/<iframe\b[^>]*>[\s\S]*?<\/iframe>/gi, '')
    .replace(/<\/?(?:script|style|iframe|object|embed|link|meta)\b[^>]*>/gi, '')
  const withoutEvents = withoutBlocked
    .replace(/\son[a-z]+\s*=\s*(['"])[\s\S]*?\1/gi, '')
    .replace(/\son[a-z]+\s*=\s*[^\s>]+/gi, '')
    .replace(/(href|src)\s*=\s*(['"])\s*javascript:[\s\S]*?\2/gi, '$1=$2$2')
  return withoutEvents.replace(/<img\b([^>]*?)\/?>/gi, (_match, attrs: string) => {
    const width = extractCssLength(attrs, 'width')
    const marginLeft = extractCssLength(attrs, 'margin-left')
    const marginTop = extractCssLength(attrs, 'margin-top')
    const cleaned = attrs
      .replace(/\sstyle\s*=\s*(['"])[\s\S]*?\1/gi, '')
      .replace(/(src)\s*=\s*(['"])([^'"]*)\2/gi, (_srcMatch: string, attr: string, quote: string, value: string) => {
        return `${attr}=${quote}${resolveMediaUrl(value)}${quote}`
      })
    const size = width ? `width:${width};` : ''
    const offset = `${marginLeft ? `margin-left:${marginLeft};` : ''}${marginTop ? `margin-top:${marginTop};` : ''}`
    return `<img${cleaned} style="${size}${offset}max-width:100%;height:auto;display:block;">`
  })
}

function extractCssLength(attrs: string, prop: string): string {
  const styleMatch = attrs.match(/\sstyle\s*=\s*(['"])([\s\S]*?)\1/i)
  if (!styleMatch) {
    return ''
  }
  const re = new RegExp(`${prop}\\s*:\\s*([^;]+)`, 'i')
  const matched = styleMatch[2].match(re)
  if (!matched) {
    return ''
  }
  const value = matched[1].trim()
  // 仅保留安全的长度值，避免注入
  return /^-?\d+(\.\d+)?(px|%|em|rem|vw)?$/i.test(value) ? value : ''
}

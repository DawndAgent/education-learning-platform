import type { Category } from '../types/category'
import type { ContentCreateRequest, ContentQuery, ContentStatus, ContentType, ContentUpdateRequest } from '../types/content'

export interface CategoryOption {
  id: string
  label: string
  leaf: boolean
}

export interface ContentFormValues {
  title: string
  contentType: ContentType | ''
  categoryId: string
  coverUrl: string
  summary: string
  sort: number | null
}

export interface ContentActions {
  edit: boolean
  publish: boolean
  offline: boolean
  remove: boolean
  copy: boolean
  preview: boolean
}

const CONTENT_TYPES: readonly ContentType[] = ['ARTICLE', 'VIDEO', 'QUESTION', 'WEEKLY', 'DOCUMENT']

export function contentTypeLabel(type: ContentType): string {
  const labels: Record<ContentType, string> = {
    ARTICLE: '文章',
    VIDEO: '视频',
    QUESTION: '题目',
    WEEKLY: '每周一题',
    DOCUMENT: '资料',
  }
  return labels[type]
}

export function displayStatusLabel(status: ContentStatus, scheduledPublishTime: string | null): string {
  if (status === 'DRAFT' && scheduledPublishTime) {
    return '待定时发布'
  }
  return statusLabel(status)
}

export function statusLabel(status: ContentStatus): string {
  if (status === 'DRAFT') {
    return '草稿'
  }
  if (status === 'PUBLISHED') {
    return '已发布'
  }
  return '已下架'
}

export function statusTagType(status: ContentStatus): 'info' | 'success' | 'warning' {
  if (status === 'PUBLISHED') {
    return 'success'
  }
  if (status === 'OFFLINE') {
    return 'warning'
  }
  return 'info'
}

export function formatTime(value: string | null): string {
  if (!value) {
    return ''
  }
  return value.replace('T', ' ').slice(0, 19)
}

export function categoryOptions(tree: Category[]): CategoryOption[] {
  const options: CategoryOption[] = []
  for (const root of tree) {
    options.push({ id: root.id, label: root.name, leaf: false })
    for (const child of root.children ?? []) {
      options.push({ id: child.id, label: `  ${child.name}`, leaf: true })
    }
  }
  return options
}

export function canOpenEditor(type: ContentType, permissions: readonly string[]): boolean {
  if (type === 'ARTICLE') {
    return permissions.includes('CONTENT_UPDATE')
  }
  if (type === 'VIDEO') {
    return permissions.includes('VIDEO_MANAGE')
  }
  return permissions.includes('CONTENT_UPDATE')
}

export function editorPath(type: ContentType, id: string): string {
  if (type === 'ARTICLE') {
    return `/articles/${id}`
  }
  if (type === 'VIDEO') {
    return `/videos/${id}`
  }
  if (type === 'QUESTION') {
    return `/questions/${id}`
  }
  if (type === 'WEEKLY') {
    return `/weeklies/${id}`
  }
  return `/documents/${id}`
}

export function createEditorPath(type: ContentType): string {
  if (type === 'ARTICLE') {
    return '/articles/create'
  }
  if (type === 'VIDEO') {
    return '/videos/create'
  }
  if (type === 'QUESTION') {
    return '/questions/create'
  }
  if (type === 'WEEKLY') {
    return '/weeklies/create'
  }
  return '/documents/create'
}

export function canDuplicate(type: ContentType, permissions: readonly string[]): boolean {
  if (type === 'ARTICLE') {
    return permissions.includes('CONTENT_CREATE')
  }
  if (type === 'VIDEO') {
    return permissions.includes('CONTENT_CREATE') && permissions.includes('VIDEO_MANAGE')
  }
  return permissions.includes('CONTENT_CREATE')
}

export function visibleActions(
  status: ContentStatus,
  permissions: readonly string[],
  contentType: ContentType = 'ARTICLE',
): ContentActions {
  const allowed = (permission: string) => permissions.includes(permission)
  return {
    edit: canOpenEditor(contentType, permissions),
    publish: status !== 'PUBLISHED' && allowed('CONTENT_PUBLISH'),
    offline: status === 'PUBLISHED' && allowed('CONTENT_OFFLINE'),
    remove: status !== 'PUBLISHED' && allowed('CONTENT_DELETE'),
    copy: canDuplicate(contentType, permissions),
    preview: allowed('CONTENT_VIEW'),
  }
}

export function saveSuccessMessage(status: ContentStatus): string {
  return status === 'DRAFT' ? '已保存草稿' : '保存成功'
}

export function publishActionLabel(status: ContentStatus): string {
  return status === 'OFFLINE' ? '重新发布' : '发布'
}

export function batchOfflineConfirmText(count: number): string {
  return `确定要下线选中的 ${count} 条内容吗？`
}

export function batchDeleteConfirmText(count: number): string {
  return `确定要删除选中的 ${count} 条内容吗？仅草稿和已下架内容可删除。`
}

export function selectableForBatchOffline(rows: readonly { status: ContentStatus }[]): boolean {
  return rows.length > 0 && rows.every((row) => row.status === 'PUBLISHED')
}

export function selectableForBatchDelete(rows: readonly { status: ContentStatus }[]): boolean {
  return rows.length > 0 && rows.every((row) => row.status !== 'PUBLISHED')
}

export function nextPageAfterDelete(pageNum: number, pageSize: number, totalBeforeDelete: number, deletedCount: number): number {
  const remaining = Math.max(0, totalBeforeDelete - deletedCount)
  if (remaining === 0) {
    return 1
  }
  const maxPage = Math.max(1, Math.ceil(remaining / pageSize))
  return Math.min(pageNum, maxPage)
}

export function validateContentForm(form: ContentFormValues, leaves: readonly string[]): string | null {
  const title = form.title.trim()
  if (!title) {
    return '标题不能为空'
  }
  if (title.length > 128) {
    return '标题长度不能超过128'
  }
  if (!CONTENT_TYPES.includes(form.contentType as ContentType)) {
    return '请选择内容类型'
  }
  if (!form.categoryId) {
    return '请选择分类'
  }
  if (!leaves.includes(form.categoryId)) {
    return '内容只能选择二级分类'
  }
  if (form.sort === null || Number.isNaN(form.sort) || !Number.isInteger(form.sort) || form.sort < 0 || form.sort > 9999) {
    return '排序范围为 0 到 9999'
  }
  if (form.coverUrl.length > 255) {
    return '封面地址长度不能超过255'
  }
  if (form.summary.length > 512) {
    return '摘要长度不能超过512'
  }
  return null
}

export function toCreatePayload(form: ContentFormValues): ContentCreateRequest {
  return {
    title: form.title.trim(),
    contentType: form.contentType as ContentType,
    categoryId: form.categoryId,
    coverUrl: form.coverUrl.trim(),
    summary: form.summary.trim(),
    sort: form.sort ?? 0,
  }
}

export function toUpdatePayload(form: ContentFormValues, contentType: ContentType): ContentUpdateRequest {
  return {
    title: form.title.trim(),
    contentType,
    categoryId: form.categoryId,
    coverUrl: form.coverUrl.trim(),
    summary: form.summary.trim(),
    sort: form.sort ?? 0,
  }
}

export function searchQuery(query: ContentQuery): ContentQuery {
  return { ...query, pageNum: 1 }
}

export function deleteConfirmText(title: string): string {
  return `确定删除内容「${title}」吗？`
}

export function publishConfirmText(title: string, typeLabel?: string, categoryName?: string): string {
  const lines = [`确认发布《${title}》`]
  if (typeLabel) {
    lines.push(`类型：${typeLabel}`)
  }
  if (categoryName) {
    lines.push(`分类：${categoryName}`)
  }
  lines.push('发布后该内容将在小程序端可见。')
  return lines.join('\n')
}

export function formatWallClock(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function publishTimeRange(preset: 'today' | '7d' | '30d', now = new Date()): { from: string; to: string } {
  const start = new Date(now)
  start.setHours(0, 0, 0, 0)
  if (preset === '7d') {
    start.setDate(start.getDate() - 6)
  }
  if (preset === '30d') {
    start.setDate(start.getDate() - 29)
  }
  const end = new Date(now)
  end.setHours(23, 59, 59, 0)
  return { from: formatWallClock(start), to: formatWallClock(end) }
}

export function offlineConfirmText(title: string): string {
  return `确认下线「${title}」？`
}

import type { ContentListVO, ContentQuery, ContentType } from '../types/content'
import { ApiError, toErrorMessage } from './error'
import { isCategoryId } from './category-tree'
import { contentDetailUrl, contentTypeLabel, formatPublishDate } from './content-view'

export const LIST_PAGE_SIZE = 10

export type ListStatus = 'loading' | 'success' | 'empty' | 'error'

export interface ContentCardModel {
  id: string
  title: string
  coverUrl: string
  typeLabel: string
  summary: string
  dateText: string
  url: string
}

export interface ListSession {
  generation: number
  loading: boolean
  refreshing: boolean
  loadingMore: boolean
  status: ListStatus
  message: string
  moreError: string
  canRetry: boolean
  items: ContentCardModel[]
  pageNum: number
  pageSize: number
  total: number
  hasMore: boolean
}

export interface ContentPagePayload {
  pageNum: number
  pageSize: number
  total: number
  records: ContentListVO[]
}

export function resolveCategoryParam(rawId: string): { ok: true; categoryId: string } | { ok: false; message: string } {
  const categoryId = rawId.trim()
  if (!categoryId) {
    return { ok: false, message: '分类参数缺失' }
  }
  if (!isCategoryId(categoryId)) {
    return { ok: false, message: '分类参数无效' }
  }
  return { ok: true, categoryId }
}

export function buildCategoryContentQuery(categoryId: string, pageNum: number, contentType?: ContentType): ContentQuery {
  const query: ContentQuery = {
    categoryId,
    pageNum,
    pageSize: LIST_PAGE_SIZE
  }
  if (contentType) {
    query.contentType = contentType
  }
  return query
}

export function toContentCard(record: ContentListVO): ContentCardModel {
  return {
    id: record.id,
    title: record.title,
    coverUrl: record.coverUrl || '',
    typeLabel: contentTypeLabel(record.contentType),
    summary: record.summary || '',
    dateText: formatPublishDate(record.publishTime),
    url: contentDetailUrl(record.id)
  }
}

export function createSession(): ListSession {
  return {
    generation: 0,
    loading: false,
    refreshing: false,
    loadingMore: false,
    status: 'loading',
    message: '',
    moreError: '',
    canRetry: true,
    items: [],
    pageNum: 1,
    pageSize: LIST_PAGE_SIZE,
    total: 0,
    hasMore: false
  }
}

export function startFirst(session: ListSession): { generation: number; session: ListSession } {
  const generation = session.generation + 1
  return {
    generation,
    session: {
      ...session,
      generation,
      loading: true,
      refreshing: false,
      loadingMore: false,
      moreError: '',
      status: session.items.length === 0 ? 'loading' : session.status,
      message: ''
    }
  }
}

export function startRefresh(session: ListSession): { generation: number; session: ListSession } {
  const generation = session.generation + 1
  return {
    generation,
    session: {
      ...session,
      generation,
      refreshing: true,
      loading: false,
      loadingMore: false,
      moreError: ''
    }
  }
}

export function startMore(session: ListSession): { generation: number; session: ListSession } | null {
  if (session.loading || session.refreshing || session.loadingMore || !session.hasMore || session.status !== 'success') {
    return null
  }
  return {
    generation: session.generation,
    session: {
      ...session,
      loadingMore: true,
      moreError: ''
    }
  }
}

export function commitPage(
  session: ListSession,
  generation: number,
  page: ContentPagePayload,
  mode: 'replace' | 'append'
): ListSession {
  if (session.generation !== generation) {
    return session
  }
  const incoming = (page.records || []).map(toContentCard)
  const items = mode === 'replace' ? mergeById([], incoming) : mergeById(session.items, incoming)
  const stalled = mode === 'append' && items.length === session.items.length
  const hasMore = !stalled && items.length < page.total
  const status: ListStatus = items.length === 0 ? 'empty' : 'success'
  return {
    ...session,
    items,
    pageNum: page.pageNum,
    pageSize: page.pageSize || session.pageSize,
    total: page.total,
    hasMore,
    status,
    message: status === 'empty' ? '暂无内容' : '',
    loading: false,
    refreshing: false,
    loadingMore: false,
    moreError: '',
    canRetry: true
  }
}

export function failMore(session: ListSession, generation: number): ListSession {
  if (session.generation !== generation) {
    return session
  }
  return {
    ...session,
    loadingMore: false,
    moreError: '加载更多失败，请重试'
  }
}

export function failLoad(session: ListSession, generation: number, message: string): ListSession {
  if (session.generation !== generation) {
    return session
  }
  if (session.items.length > 0) {
    return {
      ...session,
      loading: false,
      refreshing: false,
      loadingMore: false,
      moreError: message
    }
  }
  return {
    ...session,
    loading: false,
    refreshing: false,
    loadingMore: false,
    status: 'error',
    message,
    canRetry: true
  }
}

export function loadFailureMessage(error: unknown): string {
  if (error instanceof ApiError && (error.code === '404' || error.httpStatus === 404)) {
    return '分类不存在'
  }
  return toErrorMessage(error, '内容加载失败，请稍后重试')
}

export function invalidParamSession(message: string): ListSession {
  return {
    ...createSession(),
    status: 'error',
    message,
    canRetry: false
  }
}

function mergeById(existing: ContentCardModel[], incoming: ContentCardModel[]): ContentCardModel[] {
  const seen = new Set(existing.map((item) => item.id))
  const next = existing.slice()
  incoming.forEach((item) => {
    if (seen.has(item.id)) {
      return
    }
    seen.add(item.id)
    next.push(item)
  })
  return next
}

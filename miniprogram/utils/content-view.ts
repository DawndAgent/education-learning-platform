import type { CategoryTreeVO } from '../types/category'
import type { ContentListVO, ContentQuery, ContentType } from '../types/content'
import { findCategory, isRootParent } from './category-tree'

export interface LatestItem {
  id: string
  title: string
  coverUrl: string
  typeLabel: string
  meta: string
  summary: string
  dateText: string
  url: string
}

const CONTENT_TYPE_LABELS: Record<ContentType, string> = {
  ARTICLE: '文章',
  VIDEO: '视频',
  QUESTION: '题目',
  TOPIC: '专题',
  WEEKLY: '每周一题',
  DOCUMENT: '资料'
}

export function buildLatestQuery(categoryId?: string): ContentQuery {
  const query: ContentQuery = {
    pageNum: 1,
    pageSize: 10,
    sort: 'publishTime'
  }
  if (categoryId) {
    query.categoryId = categoryId
  }
  return query
}

export function buildSearchQuery(keyword: string, pageNum: number): ContentQuery {
  return {
    keyword: keyword.trim(),
    pageNum,
    pageSize: 10,
    sort: 'publishTime'
  }
}

export function searchPageUrl(keyword?: string): string {
  const q = (keyword || '').trim()
  if (!q) {
    return '/pages/search/search'
  }
  return `/pages/search/search?keyword=${encodeURIComponent(q)}`
}

export function contentTypeLabel(contentType: string): string {
  if (contentType in CONTENT_TYPE_LABELS) {
    return CONTENT_TYPE_LABELS[contentType as ContentType]
  }
  return ''
}

export function formatPublishDate(value: string | null): string {
  if (!value) {
    return ''
  }
  const matched = /^(\d{4}-\d{2}-\d{2})/.exec(value)
  return matched ? matched[1] : ''
}

export function categoryMeta(tree: CategoryTreeVO[], categoryId: string): string {
  const names: string[] = []
  let current = findCategory(tree, categoryId)
  const seen = new Set<string>()
  while (current && !seen.has(current.id)) {
    seen.add(current.id)
    names.unshift(current.name)
    if (isRootParent(current.parentId)) {
      break
    }
    current = findCategory(tree, current.parentId)
  }
  return names.join(' · ')
}

export function buildLatestItems(records: ContentListVO[], tree: CategoryTreeVO[]): LatestItem[] {
  return records.map((item) => ({
    id: item.id,
    title: item.title,
    coverUrl: item.coverUrl || '',
    typeLabel: contentTypeLabel(item.contentType),
    meta: categoryMeta(tree, item.categoryId),
    summary: item.summary || '',
    dateText: formatPublishDate(item.publishTime),
    url: contentDetailUrl(item.id)
  }))
}

export function contentDetailUrl(contentId: string): string {
  return `/pages/content-detail/content-detail?id=${encodeURIComponent(contentId)}`
}

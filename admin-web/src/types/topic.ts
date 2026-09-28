import type { ContentStatus, ContentType } from './content'

export type TopicStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE'

export interface Topic {
  id: string
  name: string
  code: string
  coverUrl: string | null
  summary: string | null
  categoryId: string
  categoryName: string | null
  contentCount: number
  status: TopicStatus
  sort: number
  publishTime: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface TopicContentItem {
  contentId: string
  title: string
  contentType: ContentType
  coverUrl: string | null
  categoryId: string
  categoryName: string | null
  status: ContentStatus
  sort: number
}

export interface TopicQuery {
  pageNum: number
  pageSize: number
  keyword: string
  categoryId: string
  status: TopicStatus | ''
}

export interface TopicCreateRequest {
  name: string
  code: string
  coverUrl: string
  summary: string
  categoryId: string
  sort: number
}

export interface TopicUpdateRequest {
  name: string
  coverUrl: string
  summary: string
  categoryId: string
  sort: number
}

export interface TopicItemBatchResult {
  successCount: number
  duplicateCount: number
  invalidCount: number
}

export interface TopicContentAddRequest {
  contentIds: string[]
}

export interface TopicContentSortItem {
  contentId: string
  sort: number
}

export interface TopicContentSortRequest {
  items: TopicContentSortItem[]
}

export type ContentType = 'ARTICLE' | 'VIDEO' | 'QUESTION' | 'WEEKLY' | 'DOCUMENT'

export type ContentStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE'

export interface Content {
  id: string
  title: string
  contentType: ContentType
  categoryId: string
  categoryName: string | null
  coverUrl: string | null
  summary: string | null
  status: ContentStatus
  sort: number
  viewCount: string
  favoriteCount: string
  publishTime: string | null
  scheduledPublishTime: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  records: T[]
}

export interface ContentQuery {
  pageNum: number
  pageSize: number
  keyword: string
  categoryId: string
  contentType: ContentType | ''
  status: ContentStatus | ''
  schedule?: '' | 'PLAIN' | 'SCHEDULED'
  publishTimeFrom?: string
  publishTimeTo?: string
}

export interface ContentCreateRequest {
  title: string
  contentType: ContentType
  categoryId: string
  coverUrl: string
  summary: string
  sort: number
}

export interface ContentUpdateRequest {
  title: string
  contentType: ContentType
  categoryId: string
  coverUrl: string
  summary: string
  sort: number
}

export interface ContentBatchResult {
  successCount: number
  failedCount: number
}

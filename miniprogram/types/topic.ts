import { PageResult } from './api'
import type { ContentType } from './content'

export interface TopicCategory {
  id: string
  name: string
}

export interface TopicListItem {
  id: string
  name: string
  coverUrl: string | null
  summary: string | null
  categoryId: string
  categoryName: string | null
  sort: number
  publishTime: string | null
}

/** 专题可挂载的内容类型（不含 TOPIC 自身）。 */
export type TopicContentType = Exclude<ContentType, 'TOPIC'>

export interface TopicContentItem {
  id: string
  title: string
  contentType: TopicContentType
  coverUrl: string | null
  summary: string | null
  publishTime?: string | null
}

export interface TopicDetail {
  id: string
  name: string
  coverUrl: string | null
  summary: string | null
  category: TopicCategory | null
  contents: TopicContentItem[]
}

export type TopicPage = PageResult<TopicListItem>

export interface TopicQuery {
  keyword?: string
  categoryId?: string
  pageNum?: number
  pageSize?: number
}

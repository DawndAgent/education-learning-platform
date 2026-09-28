export interface ArticleDetail {
  contentId: string
  title: string
  categoryId: string
  coverUrl: string | null
  summary: string | null
  status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
  sort: number
  publishTime: string | null
  scheduledPublishTime: string | null
  body: string
  author: string | null
  source: string | null
}

export interface ArticlePayload {
  categoryId: string
  title: string
  coverUrl: string
  summary: string
  sort: number
  body: string
  author: string
  source: string
}

export type HomeLinkType = 'CONTENT' | 'TOPIC' | 'URL' | 'NONE'
export type RecommendType = 'CONTENT' | 'TOPIC'
export type ContentType = 'ARTICLE' | 'VIDEO' | 'QUESTION' | 'TOPIC' | 'WEEKLY' | 'DOCUMENT'

export interface HomeBanner {
  id: string
  title: string
  subtitle: string | null
  imageUrl: string
  linkType: HomeLinkType
  linkId: string | null
  linkUrl: string | null
}

export interface HomeCategory {
  id: string
  name: string
  code: string
  iconUrl: string | null
}

export interface HomeRecommendation {
  id: string
  type: RecommendType
  targetId: string
  title: string
  coverUrl: string | null
  summary: string | null
  contentType: ContentType | null
}

export interface HomeLatest {
  id: string
  title: string
  contentType: ContentType
  coverUrl: string | null
  summary: string | null
  publishTime: string | null
}

export interface HomePage {
  banners: HomeBanner[]
  categories: HomeCategory[]
  recommendations: HomeRecommendation[]
  topics: HomeRecommendation[]
  latestContents: HomeLatest[]
}

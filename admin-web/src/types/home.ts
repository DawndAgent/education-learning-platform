import type { ContentStatus, ContentType } from './content'
import type { TopicStatus } from './topic'

export type HomeLinkType = 'CONTENT' | 'TOPIC' | 'URL' | 'NONE'
export type HomeItemStatus = 'ENABLED' | 'DISABLED'
export type RecommendType = 'CONTENT' | 'TOPIC'

export interface HomeBanner {
  id: string
  title: string
  subtitle: string | null
  imageUrl: string
  linkType: HomeLinkType
  linkId: string | null
  linkUrl: string | null
  targetTitle: string | null
  sort: number
  status: HomeItemStatus
  startTime: string | null
  endTime: string | null
}

export interface BannerQuery {
  pageNum: number
  pageSize: number
  keyword: string
  status: HomeItemStatus | ''
}

export interface BannerSaveRequest {
  title: string
  subtitle: string
  imageUrl: string
  linkType: HomeLinkType
  linkId: string | null
  linkUrl: string | null
  sort: number
  startTime: string | null
  endTime: string | null
}

export interface HomeRecommendation {
  id: string
  recommendType: RecommendType
  targetId: string
  title: string | null
  coverUrl: string | null
  categoryName: string | null
  contentType: ContentType | null
  contentStatus: ContentStatus | null
  topicStatus: TopicStatus | null
  sort: number
  status: HomeItemStatus
}

export interface RecommendationQuery {
  pageNum: number
  pageSize: number
  recommendType: RecommendType | ''
  status: HomeItemStatus | ''
}

export interface RecommendationCreateRequest {
  recommendType: RecommendType
  targetId: string
  sort: number
}

export interface HomeSortItem {
  id: string
  sort: number
}

export interface PublicHomeBanner {
  id: string
  title: string
  subtitle: string | null
  imageUrl: string
  linkType: HomeLinkType
  linkId: string | null
  linkUrl: string | null
}

export interface PublicHomeCategory {
  id: string
  name: string
  code: string
  iconUrl: string | null
}

export interface PublicHomeRecommendation {
  id: string
  type: RecommendType
  targetId: string
  title: string
  coverUrl: string | null
  summary: string | null
  contentType: ContentType | null
}

export interface PublicHomeLatest {
  id: string
  title: string
  contentType: ContentType
  coverUrl: string | null
  summary: string | null
  publishTime: string | null
}

export interface PublicHome {
  banners: PublicHomeBanner[]
  categories: PublicHomeCategory[]
  recommendations: PublicHomeRecommendation[]
  topics: PublicHomeRecommendation[]
  latestContents: PublicHomeLatest[]
}

export type DashboardContentType = 'ARTICLE' | 'VIDEO'

export interface DashboardCategoryStat {
  categoryId: string
  categoryName: string
  count: number
}

export interface DashboardRecentContent {
  id: string
  title: string
  contentType: DashboardContentType
  categoryName: string | null
  publishTime: string | null
}

export interface DashboardOverview {
  contentTotal: number
  publishedCount: number
  draftCount: number
  scheduledPublishCount: number
  offlineCount: number
  articleCount: number
  videoCount: number
  weekNewCount: number
  monthNewCount: number
  categoryStats: DashboardCategoryStat[]
  recentPublished: DashboardRecentContent[]
}

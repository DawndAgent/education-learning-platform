export type VideoSourceType = 'WECHAT_CHANNEL' | 'TENCENT_VIDEO' | 'LOCAL'

export interface VideoDetail {
  contentId: string
  title: string
  categoryId: string
  coverUrl: string | null
  summary: string | null
  status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
  sort: number
  publishTime: string | null
  scheduledPublishTime: string | null
  sourceType: VideoSourceType | null
  videoUrl: string | null
  qrCodeUrl: string | null
  duration: number | null
}

export interface VideoPayload {
  categoryId: string
  title: string
  coverUrl: string
  summary: string
  sort: number
  sourceType: VideoSourceType
  videoUrl: string
  qrCodeUrl: string
  duration: number | null
}

export interface MiniprogramQr {
  contentId: string
  title: string
  url: string
  mocked: boolean
}

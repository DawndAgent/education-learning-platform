import { PageResult } from './api'

export type ContentType = 'ARTICLE' | 'VIDEO' | 'QUESTION' | 'TOPIC' | 'WEEKLY' | 'DOCUMENT'
export type ContentStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
export type VideoSourceType = 'WECHAT_CHANNEL' | 'TENCENT_VIDEO' | 'LOCAL'

/**
 * 与 ContentQueryRequest 一致。
 * 公开列表会忽略 status，只返回已发布内容。
 */
export interface ContentQuery {
  categoryId?: string
  contentType?: ContentType
  status?: ContentStatus
  keyword?: string
  /** publishTime = 按发布时间倒序（首页最新 / 搜索） */
  sort?: 'publishTime'
  pageNum?: number
  pageSize?: number
}

/**
 * 与 ContentListVO 一致。
 * id、categoryId、viewCount、favoriteCount 是字符串。
 * publishTime 是 ISO-8601 字符串，未发布时为 null。
 */
export interface ContentListVO {
  id: string
  title: string
  contentType: ContentType
  categoryId: string
  coverUrl: string | null
  summary: string | null
  status: ContentStatus
  sort: number
  viewCount: string
  favoriteCount: string
  publishTime: string | null
}

/** 与 ContentDetailVO 一致。 */
export type ContentDetailVO = ContentListVO

/** 与 ArticleDetailVO 一致。 */
export interface ArticleDetailVO {
  contentId: string
  title: string
  categoryId: string
  coverUrl: string | null
  summary: string | null
  status: ContentStatus
  sort: number
  publishTime: string | null
  body: string
  author: string | null
  source: string | null
}

/** 与 VideoDetailVO 一致。 */
export interface VideoDetailVO {
  contentId: string
  title: string
  categoryId: string
  coverUrl: string | null
  summary: string | null
  status: ContentStatus
  sort: number
  publishTime: string | null
  sourceType: VideoSourceType
  videoUrl: string
  qrCodeUrl: string | null
  duration: number | null
}

export type QuestionType =
  | 'SINGLE_CHOICE'
  | 'MULTIPLE_CHOICE'
  | 'FILL_BLANK'
  | 'ANSWER'
  | 'PROOF'
  | 'IMAGE_QUESTION'

export type QuestionDifficulty = 'EASY' | 'MEDIUM' | 'HARD'

/** 与 QuestionDetailVO 一致。contentId / categoryId 为字符串。 */
export interface QuestionDetailVO {
  contentId: string
  title: string
  categoryId: string
  coverUrl: string | null
  summary: string | null
  status: ContentStatus
  sort: number
  publishTime: string | null
  questionType: QuestionType | null
  questionText: string | null
  questionImageUrl: string | null
  answerText: string | null
  answerImageUrl: string | null
  analysisText: string | null
  analysisImageUrl: string | null
  difficulty: QuestionDifficulty | null
}

/** 与 WeeklyDetailVO 一致。 */
export interface WeeklyDetailVO {
  contentId: string
  title: string
  categoryId: string
  coverUrl: string | null
  summary: string | null
  status: ContentStatus
  sort: number
  publishTime: string | null
  weekLabel: string | null
  questionText: string | null
  questionImageUrl: string | null
  answerText: string | null
  answerImageUrl: string | null
  analysisText: string | null
  analysisImageUrl: string | null
}

/** 与 DocumentDetailVO 一致。fileSize 序列化为字符串。 */
export interface DocumentDetailVO {
  contentId: string
  title: string
  categoryId: string
  coverUrl: string | null
  summary: string | null
  status: ContentStatus
  sort: number
  publishTime: string | null
  fileUrl: string | null
  fileName: string | null
  fileSize: string | null
  fileType: string | null
  downloadUrl: string | null
  previewUrl: string | null
  description: string | null
}

export type ContentPage = PageResult<ContentListVO>

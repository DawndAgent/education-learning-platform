export interface DocumentDetail {
  contentId: string
  title: string
  categoryId: string
  coverUrl: string | null
  summary: string | null
  status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
  sort: number
  publishTime: string | null
  scheduledPublishTime: string | null
  fileUrl: string | null
  fileName: string | null
  fileSize: number | null
  fileType: string | null
  downloadUrl: string | null
  previewUrl: string | null
  description: string | null
}

export interface DocumentPayload {
  categoryId: string
  title: string
  coverUrl: string
  summary: string
  sort: number
  fileUrl: string
  fileName: string
  fileSize: number | null
  fileType: string
  downloadUrl: string
  previewUrl: string
  description: string
}

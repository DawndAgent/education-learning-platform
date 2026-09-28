export interface FileUploadResult {
  url: string
  objectKey: string
  fileName: string
  contentType: string
  size: number
}

export type UploadScene = 'ARTICLE' | 'COVER' | 'VIDEO' | 'QRCODE' | 'QUESTION' | 'WEEKLY' | 'DOCUMENT'

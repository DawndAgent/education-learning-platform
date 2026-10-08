import type { ApiResponse } from '@/types/api'
import type { FileUploadResult, UploadScene } from '@/types/file'
import { validateDocumentFile, validateImageFile, validateVideoFile } from '@/utils/file-upload'
import request from './request'

export { validateDocumentFile, validateImageFile, validateVideoFile }

function uploadFile(
  file: File,
  options?: {
    scene?: UploadScene
    timeout?: number
    onProgress?: (percent: number) => void
  },
) {
  const form = new FormData()
  form.append('file', file)
  if (options?.scene) {
    form.append('scene', options.scene)
  }
  return request.post<ApiResponse<FileUploadResult>>('/admin/api/files/upload', form, {
    timeout: options?.timeout ?? 60000,
    onUploadProgress(event) {
      if (!options?.onProgress || !event.total) {
        return
      }
      const percent = Math.min(100, Math.round((event.loaded / event.total) * 100))
      options.onProgress(percent)
    },
  })
}

export function uploadImage(
  file: File,
  options?: {
    scene?: UploadScene
    onProgress?: (percent: number) => void
  },
) {
  return uploadFile(file, options)
}

export function uploadDocument(
  file: File,
  options?: {
    onProgress?: (percent: number) => void
  },
) {
  return uploadFile(file, {
    scene: 'DOCUMENT',
    onProgress: options?.onProgress,
  })
}

export function uploadVideo(
  file: File,
  options?: {
    onProgress?: (percent: number) => void
  },
) {
  return uploadFile(file, {
    scene: 'VIDEO_FILE',
    timeout: 180000,
    onProgress: options?.onProgress,
  })
}

export const fileApi = {
  uploadImage,
  uploadDocument,
  uploadVideo,
  validateImageFile,
  validateDocumentFile,
  validateVideoFile,
}

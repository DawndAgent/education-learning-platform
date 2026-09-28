import { isMediaUrl } from './media-url'
import type { VideoPayload, VideoSourceType } from '../types/video'

export interface VideoFormValues {
  title: string
  categoryId: string
  coverUrl: string
  summary: string
  sort: number | null
  sourceType: VideoSourceType | ''
  videoUrl: string
  qrCodeUrl: string
  duration: number | null
}

export function sourceTypeLabel(sourceType: VideoSourceType): string {
  return sourceType === 'WECHAT_CHANNEL' ? '微信视频号' : '腾讯视频'
}

export function formatDuration(seconds: number | null): string {
  if (seconds === null || !Number.isInteger(seconds) || seconds < 0) {
    return ''
  }
  const minutes = Math.floor(seconds / 60)
  const remain = seconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(remain).padStart(2, '0')}`
}

export function isHttpUrl(value: string): boolean {
  return isMediaUrl(value)
}

export function validateVideoForm(form: VideoFormValues, leaves: readonly string[]): string | null {
  const title = form.title.trim()
  if (!title) {
    return '标题不能为空'
  }
  if (title.length > 128) {
    return '标题长度不能超过128'
  }
  if (!form.categoryId || !leaves.includes(form.categoryId)) {
    return '内容只能选择二级分类'
  }
  if (form.sort === null || !Number.isInteger(form.sort) || form.sort < 0 || form.sort > 9999) {
    return '排序范围为 0 到 9999'
  }
  if (form.sourceType !== 'WECHAT_CHANNEL' && form.sourceType !== 'TENCENT_VIDEO') {
    return '请选择视频来源'
  }
  const videoUrlError = optionalUrl(form.videoUrl, true, '视频地址长度不能超过512', '视频地址不合法')
  if (videoUrlError) {
    return videoUrlError
  }
  const qrError = optionalUrl(form.qrCodeUrl, false, '二维码地址长度不能超过512', '二维码地址不合法')
  if (qrError) {
    return qrError
  }
  if (form.duration !== null && (!Number.isInteger(form.duration) || form.duration < 0)) {
    return '时长不能小于0'
  }
  if (form.coverUrl.length > 255) {
    return '封面地址长度不能超过255'
  }
  if (form.summary.length > 512) {
    return '摘要长度不能超过512'
  }
  return null
}

export function validateVideoPublish(form: VideoFormValues, leaves: readonly string[]): string | null {
  const draftError = validateVideoForm(form, leaves)
  if (draftError) {
    return draftError
  }
  if (!form.videoUrl.trim()) {
    return '视频地址不能为空'
  }
  if (!/^https?:\/\/\S+$/i.test(form.videoUrl.trim())) {
    return '视频地址不合法'
  }
  if (!form.qrCodeUrl.trim()) {
    return '二维码地址不能为空'
  }
  if (!isMediaUrl(form.qrCodeUrl)) {
    return '二维码地址不合法'
  }
  return null
}

function optionalUrl(
  value: string,
  absoluteOnly: boolean,
  lengthMessage: string,
  invalidMessage: string,
): string | null {
  const url = value.trim()
  if (!url) {
    return null
  }
  if (url.length > 512) {
    return lengthMessage
  }
  if (absoluteOnly) {
    if (!/^https?:\/\/\S+$/i.test(url)) {
      return invalidMessage
    }
    return null
  }
  if (!isMediaUrl(url)) {
    return invalidMessage
  }
  return null
}

export function toVideoPayload(form: VideoFormValues): VideoPayload {
  return {
    title: form.title.trim(),
    categoryId: form.categoryId,
    coverUrl: form.coverUrl.trim(),
    summary: form.summary.trim(),
    sort: form.sort ?? 0,
    sourceType: form.sourceType === 'TENCENT_VIDEO' ? 'TENCENT_VIDEO' : 'WECHAT_CHANNEL',
    videoUrl: form.videoUrl.trim(),
    qrCodeUrl: form.qrCodeUrl.trim(),
    duration: form.duration,
  }
}

export function canPreviewQr(url: string): boolean {
  return isMediaUrl(url)
}

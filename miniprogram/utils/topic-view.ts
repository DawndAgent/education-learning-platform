import { contentTypeLabel } from './content-view'

export function topicListUrl(): string {
  return '/pages/topic-list/topic-list'
}

export function topicDetailUrl(id: string): string {
  return `/pages/topic-detail/topic-detail?id=${encodeURIComponent(id)}`
}

export function topicTypeLabel(contentType: string): string {
  return contentTypeLabel(contentType)
}

import { apiBaseUrl } from '../config/env'

export function resolveMediaUrl(url: string, apiBase = apiBaseUrl): string {
  const trimmed = url.trim()
  if (!trimmed) {
    return ''
  }
  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed
  }
  if (trimmed.startsWith('/') && !trimmed.startsWith('//')) {
    return `${apiBase.replace(/\/$/, '')}${trimmed}`
  }
  return trimmed
}

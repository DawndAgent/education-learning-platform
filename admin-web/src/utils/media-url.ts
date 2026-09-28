export function resolveMediaUrl(url: string, apiBase = 'http://localhost:8080'): string {
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

export function isMediaUrl(value: string): boolean {
  const trimmed = value.trim()
  if (!trimmed) {
    return false
  }
  if (/^https?:\/\/\S+$/i.test(trimmed)) {
    return true
  }
  return trimmed.startsWith('/') && !trimmed.startsWith('//') && !trimmed.includes('..')
}

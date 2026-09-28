import { getStorage, removeStorage, setStorage } from './storage'

const KEY = 'mp_search_history'
const MAX = 10

export function readSearchHistory(): string[] {
  const raw = getStorage(KEY)
  if (!raw) {
    return []
  }
  try {
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed
      .filter((item): item is string => typeof item === 'string')
      .map((item) => item.trim())
      .filter((item) => item.length > 0)
      .slice(0, MAX)
  } catch {
    return []
  }
}

export function pushSearchHistory(keyword: string): string[] {
  const value = keyword.trim()
  if (!value) {
    return readSearchHistory()
  }
  const next = [value, ...readSearchHistory().filter((item) => item !== value)].slice(0, MAX)
  setStorage(KEY, JSON.stringify(next))
  return next
}

export function clearSearchHistory(): void {
  removeStorage(KEY)
}

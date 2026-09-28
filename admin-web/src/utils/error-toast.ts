/** 短窗口错误提示去重，避免并行请求刷屏 */
let lastErrorToast = { message: '', at: 0 }

export const ERROR_TOAST_DEDUP_MS = 800

export function shouldShowErrorToast(message: string, now = Date.now()): boolean {
  if (message === lastErrorToast.message && now - lastErrorToast.at < ERROR_TOAST_DEDUP_MS) {
    return false
  }
  lastErrorToast = { message, at: now }
  return true
}

/** 仅测试用：重置去重窗口 */
export function resetErrorToastWindow(): void {
  lastErrorToast = { message: '', at: 0 }
}

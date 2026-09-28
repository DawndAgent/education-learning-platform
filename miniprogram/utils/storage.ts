export function getStorage(key: string): string {
  const value: unknown = wx.getStorageSync(key)
  return typeof value === 'string' ? value : ''
}

export function setStorage(key: string, value: string): void {
  wx.setStorageSync(key, value)
}

export function removeStorage(key: string): void {
  wx.removeStorageSync(key)
}

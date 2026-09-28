import { apiBaseUrl, requestTimeoutMs } from '../config/env'
import { ApiError, readApiBody } from '../utils/error'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

function send<T>(method: HttpMethod, url: string, data?: object): Promise<T> {
  return new Promise((resolve, reject) => {
    wx.request({
      url: joinUrl(apiBaseUrl, url),
      method,
      data: compact(data),
      timeout: requestTimeoutMs,
      header: {
        'content-type': 'application/json'
      },
      success(response) {
        try {
          resolve(readApiBody<T>(response.statusCode, response.data))
        } catch (error) {
          reject(error)
        }
      },
      fail(error) {
        reject(new ApiError('NETWORK', error.errMsg || '网络异常'))
      }
    })
  })
}

export const request = {
  get<T>(url: string, params?: object): Promise<T> {
    return send<T>('GET', url, params)
  },

  post<T>(url: string, data?: object): Promise<T> {
    return send<T>('POST', url, data)
  },

  put<T>(url: string, data?: object): Promise<T> {
    return send<T>('PUT', url, data)
  },

  delete<T>(url: string): Promise<T> {
    return send<T>('DELETE', url)
  }
}

function joinUrl(baseUrl: string, path: string): string {
  const base = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl
  const suffix = path.startsWith('/') ? path : `/${path}`
  return `${base}${suffix}`
}

function compact(data?: object): Record<string, string | number | boolean> | undefined {
  if (!data) {
    return undefined
  }
  const result: Record<string, string | number | boolean> = {}
  Object.entries(data as Record<string, unknown>).forEach(([key, value]) => {
    if (typeof value === 'string' && value !== '') {
      result[key] = value
    } else if (typeof value === 'number' && Number.isFinite(value)) {
      result[key] = value
    } else if (typeof value === 'boolean') {
      result[key] = value
    }
  })
  return result
}

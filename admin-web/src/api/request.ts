import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/api'
import { shouldLeaveAfterUnauthorized } from '@/utils/access'
import { shouldShowErrorToast } from '@/utils/error-toast'
import { clearToken, getToken } from '@/utils/token'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
})

function toastError(message: string) {
  if (!shouldShowErrorToast(message)) {
    return
  }
  ElMessage.error(message)
}

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as Partial<ApiResponse<unknown>> | undefined
    if (body && typeof body.code === 'string' && body.code !== '0') {
      const message = safeMessage(body.message, '业务处理失败')
      toastError(message)
      return Promise.reject(new Error(message))
    }
    return response
  },
  async (error: AxiosError<Partial<ApiResponse<unknown>>>) => {
    const message = resolveHttpMessage(error)
    const status = error.response?.status
    const url = error.config?.url || ''
    if (status === 401 && shouldLeaveAfterUnauthorized(url)) {
      clearToken()
      try {
        const { useAuthStore } = await import('@/stores/auth')
        useAuthStore().clearSession()
      } catch {
        // pinia 未就绪时仅清 token
      }
      const { default: router } = await import('@/router')
      if (router.currentRoute.value.path !== '/login') {
        await router.replace('/login')
      }
    }
    toastError(message)
    return Promise.reject(new Error(message))
  },
)

function resolveHttpMessage(error: AxiosError<Partial<ApiResponse<unknown>>>): string {
  if (!error.response) {
    return '网络错误'
  }
  const status = error.response.status
  if (status === 403) {
    return safeMessage(error.response.data?.message, '没有权限')
  }
  if (status >= 500) {
    return '服务器错误'
  }
  if (status >= 400) {
    return safeMessage(error.response.data?.message, '请求错误')
  }
  return '请求失败'
}

function safeMessage(message: string | undefined, fallback: string): string {
  if (!message || /exception|sql|java\.|org\./i.test(message)) {
    return fallback
  }
  return message
}

export default request

import type { ApiResponse, FieldError } from '../types/api'

export class ApiError extends Error {
  readonly code: string
  readonly httpStatus: number | null
  readonly fieldErrors: FieldError[]

  constructor(code: string, message: string, httpStatus: number | null = null, fieldErrors: FieldError[] = []) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.httpStatus = httpStatus
    this.fieldErrors = fieldErrors
  }
}

export function readApiBody<T>(httpStatus: number, body: unknown): T {
  if (!isApiResponse(body)) {
    throw new ApiError(String(httpStatus), '服务响应无法解析', httpStatus)
  }
  if (httpStatus >= 200 && httpStatus < 300 && body.code === '0') {
    return body.data as T
  }
  throw new ApiError(body.code, body.message || '请求失败', httpStatus, readFieldErrors(body.data))
}

const INTERNAL_MESSAGE = /exception|sql|nullpointer|stacktrace|java\.|org\./i

export function toErrorMessage(error: unknown, fallback = '内容加载失败，请稍后重试'): string {
  if (!(error instanceof ApiError)) {
    return '网络异常，请稍后重试'
  }
  if (error.code === 'NETWORK') {
    return '网络异常，请稍后重试'
  }
  if (error.code === '500' || error.httpStatus === 500 || INTERNAL_MESSAGE.test(error.message)) {
    return fallback
  }
  return error.message || fallback
}

function isApiResponse(body: unknown): body is ApiResponse<unknown> {
  if (body === null || typeof body !== 'object') {
    return false
  }
  const record = body as Record<string, unknown>
  return typeof record.code === 'string' && typeof record.message === 'string' && 'data' in record
}

function readFieldErrors(data: unknown): FieldError[] {
  if (!Array.isArray(data)) {
    return []
  }
  return data.filter(isFieldError)
}

function isFieldError(value: unknown): value is FieldError {
  if (value === null || typeof value !== 'object') {
    return false
  }
  const record = value as Record<string, unknown>
  return typeof record.field === 'string' && typeof record.message === 'string'
}

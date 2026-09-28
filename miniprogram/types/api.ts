export type PageStatus = 'loading' | 'success' | 'empty' | 'error'

/** 与后端 ApiResponse 一致。code 为字符串，成功时是 "0"。 */
export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

/** 参数校验失败时，data 是字段错误列表。 */
export interface FieldError {
  field: string
  message: string
}

/**
 * 与后端 PageResult 一致。
 * pageNum、pageSize、total 是 JSON 数字。
 */
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  records: T[]
}

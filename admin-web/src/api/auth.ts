import type { ApiResponse } from '@/types/api'
import type { CurrentUser, LoginResult } from '@/types/auth'
import request from './request'

export function login(username: string, password: string) {
  return request.post<ApiResponse<LoginResult>>('/admin/api/auth/login', { username, password })
}

export function currentUser() {
  return request.get<ApiResponse<CurrentUser>>('/admin/api/auth/me')
}

export function logout() {
  return request.post<ApiResponse<null>>('/admin/api/auth/logout')
}

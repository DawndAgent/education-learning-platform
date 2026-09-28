import { defineStore } from 'pinia'
import { currentUser, login as loginRequest, logout as logoutRequest } from '@/api/auth'
import type { CurrentUser } from '@/types/auth'
import { clearToken, getToken, setToken } from '@/utils/token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: null as CurrentUser | null,
  }),
  getters: {
    permissions: (state): string[] => state.user?.permissions ?? [],
  },
  actions: {
    hasPermission(permission: string): boolean {
      return this.permissions.includes(permission)
    },
    hasAnyPermission(permissions: string[]): boolean {
      return permissions.some((permission) => this.hasPermission(permission))
    },
    async login(username: string, password: string) {
      const response = await loginRequest(username, password)
      const result = response.data.data
      setToken(result.token)
      this.token = result.token
      this.user = result.user
    },
    async loadCurrentUser() {
      const response = await currentUser()
      this.user = response.data.data
      this.token = getToken()
    },
    async logout() {
      try {
        await logoutRequest()
      } finally {
        this.clearSession()
      }
    },
    clearSession() {
      clearToken()
      this.token = ''
      this.user = null
    },
  },
})

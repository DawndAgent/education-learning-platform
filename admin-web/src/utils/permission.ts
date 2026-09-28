import { useAuthStore } from '@/stores/auth'
import { requiredMenuPermission } from '@/utils/access'

export function hasPermission(permission: string): boolean {
  return useAuthStore().hasPermission(permission)
}

export function hasAnyPermission(permissions: string[]): boolean {
  return useAuthStore().hasAnyPermission(permissions)
}

export function canAccessMenu(path: string): boolean {
  const required = requiredMenuPermission(path)
  if (!required) {
    return true
  }
  return hasPermission(required)
}

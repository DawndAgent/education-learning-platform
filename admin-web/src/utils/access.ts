const MENU_PERMISSION: Record<string, string> = {
  '/dashboard': 'DASHBOARD_VIEW',
  '/contents': 'CONTENT_VIEW',
  '/topics': 'TOPIC_VIEW',
  '/home-operation': 'HOME_OPERATION_VIEW',
  '/categories': 'CATEGORY_MANAGE',
  '/articles': 'CONTENT_VIEW',
  '/videos': 'VIDEO_MANAGE',
  '/questions': 'CONTENT_VIEW',
  '/weeklies': 'CONTENT_VIEW',
  '/documents': 'CONTENT_VIEW',
}

/** 路由级权限：命中任一即可进入 */
export function routePermissions(meta: { permissions?: string[] } | undefined): string[] | null {
  if (!meta || !Array.isArray(meta.permissions) || meta.permissions.length === 0) {
    return null
  }
  return meta.permissions
}

export function canAccessRoute(permissions: readonly string[], required: readonly string[]): boolean {
  return required.some((item) => permissions.includes(item))
}

export function requiredMenuPermission(path: string): string | null {
  return MENU_PERMISSION[path] || null
}

export function canSeeMenu(path: string, permissions: readonly string[]): boolean {
  const required = requiredMenuPermission(path)
  if (!required) {
    return true
  }
  return permissions.includes(required)
}

export function nextRoute(path: string, requiresAuth: boolean, hasToken: boolean): string | null {
  if (path === '/login') {
    return hasToken ? '/dashboard' : null
  }
  if (requiresAuth && !hasToken) {
    return '/login'
  }
  return null
}

export function shouldLeaveAfterUnauthorized(requestUrl: string): boolean {
  return !requestUrl.includes('/auth/login')
}

/** 侧栏高亮：编辑子路由归到对应一级菜单 */
export function activeMenuPath(path: string): string {
  if (path === '/articles' || path.startsWith('/articles/')) {
    return '/articles'
  }
  if (path === '/videos' || path.startsWith('/videos/')) {
    return '/videos'
  }
  if (path === '/topics' || path.startsWith('/topics/')) {
    return '/topics'
  }
  if (path === '/home-operation' || path.startsWith('/home-operation/')) {
    return '/home-operation'
  }
  if (
    path === '/questions'
    || path.startsWith('/questions/')
    || path === '/weeklies'
    || path.startsWith('/weeklies/')
    || path === '/documents'
    || path.startsWith('/documents/')
  ) {
    return '/contents'
  }
  return path
}

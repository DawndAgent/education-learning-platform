import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { canAccessRoute, nextRoute, routePermissions } from '@/utils/access'
import { getToken } from '@/utils/token'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录', requiresAuth: false },
    },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'contents',
          name: 'contents',
          component: () => import('@/views/contents/index.vue'),
          meta: { title: '内容管理' },
        },
        {
          path: 'topics',
          name: 'topics',
          component: () => import('@/views/topics/index.vue'),
          meta: { title: '专题管理' },
        },
        {
          path: 'topics/create',
          name: 'topic-create',
          component: () => import('@/views/topics/editor.vue'),
          meta: { title: '新增专题', permissions: ['TOPIC_CREATE'] },
        },
        {
          path: 'topics/:id/edit',
          name: 'topic-editor',
          component: () => import('@/views/topics/editor.vue'),
          meta: { title: '编辑专题', permissions: ['TOPIC_VIEW', 'TOPIC_UPDATE', 'TOPIC_CONTENT_MANAGE'] },
        },
        {
          path: 'home-operation',
          name: 'home-operation',
          component: () => import('@/views/home-operation/index.vue'),
          meta: {
            title: '首页运营',
            permissions: ['HOME_OPERATION_VIEW', 'HOME_OPERATION_MANAGE'],
          },
        },
        {
          path: 'categories',
          name: 'categories',
          component: () => import('@/views/categories/index.vue'),
          meta: { title: '分类管理' },
        },
        {
          path: 'articles',
          name: 'articles',
          component: () => import('@/views/contents/index.vue'),
          props: { fixedType: 'ARTICLE' },
          meta: { title: '文章管理' },
        },
        {
          path: 'articles/create',
          name: 'article-create',
          component: () => import('@/views/articles/editor.vue'),
          meta: { title: '新增文章', permissions: ['CONTENT_CREATE'] },
        },
        {
          path: 'articles/:id',
          name: 'article-editor',
          component: () => import('@/views/articles/editor.vue'),
          meta: { title: '编辑文章', permissions: ['CONTENT_VIEW', 'CONTENT_UPDATE'] },
        },
        {
          path: 'videos',
          name: 'videos',
          component: () => import('@/views/contents/index.vue'),
          props: { fixedType: 'VIDEO' },
          meta: { title: '视频管理' },
        },
        {
          path: 'videos/create',
          name: 'video-create',
          component: () => import('@/views/videos/editor.vue'),
          meta: { title: '新增视频', permissions: ['VIDEO_MANAGE'] },
        },
        {
          path: 'videos/:id',
          name: 'video-editor',
          component: () => import('@/views/videos/editor.vue'),
          meta: { title: '编辑视频', permissions: ['CONTENT_VIEW', 'VIDEO_MANAGE'] },
        },
        {
          path: 'questions',
          name: 'questions',
          component: () => import('@/views/contents/index.vue'),
          props: { fixedType: 'QUESTION' },
          meta: { title: '题目管理' },
        },
        {
          path: 'questions/create',
          name: 'question-create',
          component: () => import('@/views/questions/editor.vue'),
          meta: { title: '新增题目', permissions: ['CONTENT_CREATE'] },
        },
        {
          path: 'questions/import-pdf',
          name: 'question-import-pdf',
          component: () => import('@/views/questions/import-pdf.vue'),
          meta: { title: '从 PDF 导入', permissions: ['CONTENT_CREATE'] },
        },
        {
          path: 'questions/:id',
          name: 'question-editor',
          component: () => import('@/views/questions/editor.vue'),
          meta: { title: '编辑题目', permissions: ['CONTENT_VIEW', 'CONTENT_UPDATE'] },
        },
        {
          path: 'weeklies',
          name: 'weeklies',
          component: () => import('@/views/contents/index.vue'),
          props: { fixedType: 'WEEKLY' },
          meta: { title: '每周一题' },
        },
        {
          path: 'weeklies/create',
          name: 'weekly-create',
          component: () => import('@/views/weeklies/editor.vue'),
          meta: { title: '新增每周一题', permissions: ['CONTENT_CREATE'] },
        },
        {
          path: 'weeklies/:id',
          name: 'weekly-editor',
          component: () => import('@/views/weeklies/editor.vue'),
          meta: { title: '编辑每周一题', permissions: ['CONTENT_VIEW', 'CONTENT_UPDATE'] },
        },
        {
          path: 'documents',
          name: 'documents',
          component: () => import('@/views/contents/index.vue'),
          props: { fixedType: 'DOCUMENT' },
          meta: { title: '资料管理' },
        },
        {
          path: 'documents/create',
          name: 'document-create',
          component: () => import('@/views/documents/editor.vue'),
          meta: { title: '新增资料', permissions: ['CONTENT_CREATE'] },
        },
        {
          path: 'documents/:id',
          name: 'document-editor',
          component: () => import('@/views/documents/editor.vue'),
          meta: { title: '编辑资料', permissions: ['CONTENT_VIEW', 'CONTENT_UPDATE'] },
        },
      ],
    },
    {
      path: '/404',
      name: 'not-found',
      component: () => import('@/views/error/404.vue'),
      meta: { title: '页面不存在' },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/404',
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  const hasToken = Boolean(getToken())
  if (!hasToken) {
    authStore.clearSession()
  }
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth === true)
  const redirect = nextRoute(to.path, requiresAuth, hasToken)
  if (redirect) {
    return redirect
  }
  if (!requiresAuth || !hasToken) {
    return true
  }
  if (!authStore.user) {
    try {
      await authStore.loadCurrentUser()
    } catch {
      authStore.clearSession()
      return '/login'
    }
  }
  for (const record of to.matched) {
    const required = routePermissions(record.meta as { permissions?: string[] })
    if (required && !canAccessRoute(authStore.permissions, required)) {
      return '/dashboard'
    }
  }
  return true
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : '教育内容管理平台'
  document.title = `${title} - 教育内容管理平台`
})

export default router

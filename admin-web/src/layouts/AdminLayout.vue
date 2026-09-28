<script setup lang="ts">
import { Collection, Document, Folder, House, Picture, Reading, VideoCamera } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { activeMenuPath } from '@/utils/access'
import { canAccessMenu } from '@/utils/permission'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

const nickname = computed(() => authStore.user?.nickname || '管理员')
const menuActive = computed(() => activeMenuPath(route.path))
const visibleMenus = computed(() => {
  const permissions = authStore.permissions
  return menus.filter((item) => permissions && canAccessMenu(item.path))
})

async function onLogout() {
  await authStore.logout()
  await router.replace('/login')
}

const pageTitle = computed(() => {
  const title = route.meta.title
  return typeof title === 'string' ? title : ''
})

const menus = [
  { path: '/dashboard', label: '首页', icon: House },
  { path: '/contents', label: '内容管理', icon: Document },
  { path: '/topics', label: '专题管理', icon: Collection },
  { path: '/home-operation', label: '首页运营', icon: Picture },
  { path: '/categories', label: '分类管理', icon: Folder },
  { path: '/articles', label: '文章管理', icon: Reading },
  { path: '/videos', label: '视频管理', icon: VideoCamera },
]
</script>

<template>
  <el-container class="admin-shell">
    <el-header class="admin-header" height="56px">
      <div class="brand">{{ appStore.platformName }}</div>
      <div class="admin-user">
        <span>{{ nickname }}</span>
        <el-button link type="primary" @click="onLogout">退出登录</el-button>
      </div>
    </el-header>
    <el-container class="admin-body">
      <el-aside class="admin-aside" width="220px">
        <el-menu :key="menuActive" :default-active="menuActive" router>
          <el-menu-item v-for="item in visibleMenus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="admin-main">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.path !== '/dashboard'">{{ pageTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
        <h1 class="page-title">{{ pageTitle }}</h1>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-shell {
  height: 100vh;
  overflow: hidden;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e8eb;
  background: #ffffff;
}

.brand {
  font-size: 16px;
  font-weight: 600;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #646a73;
}

.admin-body {
  min-height: 0;
}

.admin-aside {
  overflow: auto;
  border-right: 1px solid #e6e8eb;
  background: #ffffff;
}

.admin-aside :deep(.el-menu) {
  border-right: none;
}

.admin-main {
  min-width: 0;
  overflow: auto;
  background: #f5f7fa;
}

.page-title {
  margin: 16px 0;
  font-size: 20px;
  font-weight: 600;
}
</style>

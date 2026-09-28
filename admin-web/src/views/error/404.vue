<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function goHome() {
  if (authStore.hasPermission('DASHBOARD_VIEW')) {
    void router.push('/dashboard')
    return
  }
  if (authStore.hasPermission('CONTENT_VIEW')) {
    void router.push('/contents')
    return
  }
  if (authStore.hasPermission('CATEGORY_MANAGE')) {
    void router.push('/categories')
    return
  }
  void router.push('/login')
}
</script>

<template>
  <main class="not-found">
    <p class="code">404</p>
    <p class="text">页面不存在</p>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </main>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  margin: 0;
  background: #f5f7fa;
}

.code {
  margin: 0;
  font-size: 56px;
  font-weight: 600;
}

.text {
  margin: 8px 0 24px;
  color: #646a73;
}
</style>

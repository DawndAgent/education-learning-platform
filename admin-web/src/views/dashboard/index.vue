<script setup lang="ts">
import { Document, Folder, Refresh, Reading, VideoCamera } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardApi } from '@/api/dashboard'
import { useAuthStore } from '@/stores/auth'
import type { DashboardOverview, DashboardRecentContent } from '@/types/dashboard'
import { contentTypeLabel } from '@/utils/content-form'
import { hasPermission } from '@/utils/permission'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const error = ref('')
const data = ref<DashboardOverview | null>(null)

const nickname = computed(() => authStore.user?.nickname || authStore.user?.username || '管理员')
const todayText = computed(() => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
})

const isEmpty = computed(() => data.value !== null && data.value.contentTotal === 0)

const categoryMax = computed(() => {
  if (!data.value || data.value.categoryStats.length === 0) {
    return 1
  }
  return Math.max(...data.value.categoryStats.map((item) => item.count), 1)
})

const canCreateContent = computed(() => hasPermission('CONTENT_CREATE'))
const canCreateVideo = computed(() => hasPermission('VIDEO_MANAGE'))
const canManageCategory = computed(() => hasPermission('CATEGORY_MANAGE'))
const canViewContent = computed(() => hasPermission('CONTENT_VIEW'))

const statusCards = computed(() => {
  const overview = data.value
  if (!overview) {
    return []
  }
  return [
    { label: '内容总数', value: overview.contentTotal, hint: '全部有效内容' },
    { label: '已发布', value: overview.publishedCount, hint: '小程序可见' },
    { label: '草稿', value: overview.draftCount, hint: '含待定时发布' },
    { label: '待定时发布', value: overview.scheduledPublishCount, hint: '草稿且已设发布时间' },
    { label: '已下线', value: overview.offlineCount, hint: '已下架内容' },
  ]
})

const typeCards = computed(() => {
  const overview = data.value
  if (!overview) {
    return []
  }
  return [
    { label: '文章', value: overview.articleCount, hint: 'ARTICLE 内容' },
    { label: '视频', value: overview.videoCount, hint: 'VIDEO 内容' },
    { label: '本周新增', value: overview.weekNewCount, hint: '按创建时间' },
    { label: '本月新增', value: overview.monthNewCount, hint: '按创建时间' },
  ]
})

function categoryPercent(count: number): number {
  const total = data.value?.contentTotal ?? 0
  if (total <= 0) {
    return 0
  }
  return Math.round((count / total) * 100)
}

function formatTime(value: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

function editPath(row: DashboardRecentContent): string {
  return row.contentType === 'VIDEO' ? `/videos/${row.id}` : `/articles/${row.id}`
}

async function loadOverview() {
  loading.value = true
  error.value = ''
  try {
    const response = await dashboardApi.getOverview()
    data.value = response.data.data
  } catch {
    data.value = null
    error.value = '数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadOverview()
})
</script>

<template>
  <section class="dashboard-page">
    <div class="welcome">
      <div>
        <h2 class="welcome-title">欢迎回来，{{ nickname }}</h2>
        <p class="welcome-date">今天是 {{ todayText }}</p>
      </div>
      <div class="welcome-actions">
        <el-button v-if="canViewContent" @click="router.push({ path: '/contents', query: { schedule: 'SCHEDULED' } })">
          待发布内容
        </el-button>
        <el-button :icon="Refresh" :loading="loading" @click="loadOverview">刷新</el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" animated :rows="8" />

    <el-result
      v-else-if="error"
      icon="error"
      title="数据加载失败"
      sub-title="无法获取运营看板数据，请稍后重试"
    >
      <template #extra>
        <el-button type="primary" @click="loadOverview">重新加载</el-button>
      </template>
    </el-result>

    <template v-else-if="data">
      <el-empty v-if="isEmpty" description="暂无内容数据" />

      <template v-else>
        <el-row :gutter="16" class="stat-row">
          <el-col v-for="item in statusCards" :key="item.label" :xs="24" :sm="12" :md="6">
            <el-card shadow="never" class="stat-card">
              <div class="stat-label">{{ item.label }}</div>
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-hint">{{ item.hint }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="stat-row">
          <el-col v-for="item in typeCards" :key="item.label" :xs="24" :sm="12" :md="6">
            <el-card shadow="never" class="stat-card">
              <div class="stat-label">{{ item.label }}</div>
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-hint">{{ item.hint }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="panel-row">
          <el-col :xs="24" :md="10">
            <el-card shadow="never">
              <template #header>
                <span>内容分类分布</span>
              </template>
              <el-empty v-if="data.categoryStats.length === 0" description="暂无分类数据" :image-size="64" />
              <div v-else class="category-list">
                <div v-for="item in data.categoryStats" :key="item.categoryId" class="category-item">
                  <div class="category-meta">
                    <span>{{ item.categoryName }}</span>
                    <span>{{ item.count }} 条 · {{ categoryPercent(item.count) }}%</span>
                  </div>
                  <el-progress
                    :percentage="Math.round((item.count / categoryMax) * 100)"
                    :stroke-width="10"
                    :show-text="false"
                  />
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="14">
            <el-card shadow="never">
              <template #header>
                <span>最近发布</span>
              </template>
              <el-empty
                v-if="data.recentPublished.length === 0"
                description="暂无已发布内容"
                :image-size="64"
              />
              <el-table v-else :data="data.recentPublished" stripe>
                <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
                <el-table-column label="类型" width="90">
                  <template #default="{ row }">
                    <el-tag size="small">{{ contentTypeLabel(row.contentType) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="categoryName" label="分类" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ row.categoryName || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="发布时间" width="170">
                  <template #default="{ row }">
                    {{ formatTime(row.publishTime) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="router.push(editPath(row))">查看</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <el-card shadow="never" class="shortcut-card">
        <template #header>
          <span>快捷操作</span>
        </template>
        <div class="shortcut-actions">
          <el-button
            v-if="canCreateContent"
            type="primary"
            :icon="Reading"
            @click="router.push('/articles/create')"
          >
            新建文章
          </el-button>
          <el-button
            v-if="canCreateVideo"
            type="primary"
            :icon="VideoCamera"
            @click="router.push('/videos/create')"
          >
            新建视频
          </el-button>
          <el-button v-if="canViewContent" :icon="Document" @click="router.push('/contents')">
            内容管理
          </el-button>
          <el-button v-if="canManageCategory" :icon="Folder" @click="router.push('/categories')">
            分类管理
          </el-button>
          <span
            v-if="!canCreateContent && !canCreateVideo && !canViewContent && !canManageCategory"
            class="shortcut-empty"
          >
            暂无可用快捷操作
          </span>
        </div>
      </el-card>
    </template>
  </section>
</template>

<style scoped>
.dashboard-page {
  min-width: 0;
}

.welcome {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.welcome-actions {
  display: flex;
  gap: 8px;
}

.welcome-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.welcome-date {
  margin: 6px 0 0;
  color: #646a73;
}

.stat-row,
.panel-row {
  margin-bottom: 16px;
}

.stat-card {
  margin-bottom: 16px;
}

.stat-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #646a73;
}

.stat-value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
}

.stat-hint {
  margin-top: 6px;
  color: #8f959e;
  font-size: 12px;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.category-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
  font-size: 13px;
}

.shortcut-card {
  margin-top: 0;
}

.shortcut-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.shortcut-empty {
  color: #8f959e;
}
</style>

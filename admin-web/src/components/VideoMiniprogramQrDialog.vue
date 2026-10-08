<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { contentApi } from '@/api/content'
import { videoApi } from '@/api/video'
import type { Content } from '@/types/content'
import { resolveMediaUrl } from '@/utils/media-url'

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const visible = defineModel<boolean>({ required: true })

const emit = defineEmits<{
  insert: [payload: { contentId: string; title: string; url: string; mocked: boolean }]
}>()

const keyword = ref('')
const loading = ref(false)
const inserting = ref(false)
const forceRegenerate = ref(false)
const loadError = ref('')
const records = ref<Content[]>([])
const selectedId = ref('')

watch(visible, (open) => {
  if (open) {
    selectedId.value = ''
    forceRegenerate.value = false
    void loadVideos()
  }
})

async function loadVideos() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await contentApi.getContentList({
      pageNum: 1,
      pageSize: 50,
      keyword: keyword.value.trim(),
      categoryId: '',
      contentType: 'VIDEO',
      status: 'PUBLISHED',
    })
    records.value = response.data.data.records
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载视频失败'
    records.value = []
  } finally {
    loading.value = false
  }
}

function onRowClick(row: Content) {
  selectedId.value = row.id
}

async function onConfirm() {
  if (!selectedId.value || inserting.value) {
    return
  }
  const item = records.value.find((row) => row.id === selectedId.value)
  inserting.value = true
  try {
    const response = await videoApi.createMiniprogramQr(selectedId.value, forceRegenerate.value)
    const data = response.data.data
    emit('insert', {
      contentId: String(data.contentId),
      title: data.title || item?.title || '',
      url: data.url,
      mocked: data.mocked,
    })
    if (data.mocked) {
      ElMessage.warning('当前为测试二维码（mock），微信扫码不会打开小程序；生产请配置 AppSecret')
    } else {
      ElMessage.success('已插入小程序码')
    }
    visible.value = false
  } catch {
    // 请求拦截器已展示错误
  } finally {
    inserting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="插入视频小程序码" width="560px" destroy-on-close>
    <p class="hint">选择已发布视频，将生成扫码打开小程序视频详情页的码，插入到正文光标处。</p>
    <div class="search">
      <el-input v-model="keyword" clearable placeholder="搜索视频标题" @keyup.enter="loadVideos" />
      <el-button :loading="loading" @click="loadVideos">搜索</el-button>
    </div>
    <el-alert v-if="loadError" type="error" :closable="false" show-icon :title="loadError" class="error" />
    <el-table
      v-loading="loading"
      :data="records"
      highlight-current-row
      max-height="320"
      empty-text="暂无已发布视频"
      @row-click="onRowClick"
    >
      <el-table-column label="" width="48" align="center">
        <template #default="{ row }">
          <el-radio v-model="selectedId" :value="row.id">&nbsp;</el-radio>
        </template>
      </el-table-column>
      <el-table-column label="封面" width="72">
        <template #default="{ row }">
          <img
            v-if="row.coverUrl"
            class="cover"
            :src="resolveMediaUrl(row.coverUrl, apiBase)"
            alt=""
          >
          <span v-else class="cover-empty">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
    </el-table>
    <template #footer>
      <el-checkbox v-model="forceRegenerate" class="force">强制重新生成小程序码</el-checkbox>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="inserting" :disabled="!selectedId" @click="onConfirm">
        插入到正文
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.hint {
  margin: 0 0 12px;
  color: #666;
  font-size: 13px;
  line-height: 1.5;
}

.search {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.error {
  margin-bottom: 12px;
}

.cover {
  width: 48px;
  height: 36px;
  object-fit: cover;
  border-radius: 2px;
}

.cover-empty {
  color: #999;
}

.force {
  margin-right: auto;
}
</style>

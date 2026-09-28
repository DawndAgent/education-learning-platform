<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/category'
import { topicApi } from '@/api/topic'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types/category'
import type { Topic, TopicContentItem, TopicQuery } from '@/types/topic'
import { normalizeTree } from '@/utils/category-form'
import {
  categoryOptions,
  contentTypeLabel,
  formatTime,
  nextPageAfterDelete,
  statusLabel,
  statusTagType,
} from '@/utils/content-form'
import {
  deleteConfirmText,
  offlineConfirmText,
  publishConfirmText,
  searchQuery,
  visibleActions,
} from '@/utils/topic-form'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const publishingId = ref('')
const offliningId = ref('')
const deletingId = ref('')
const loadError = ref('')
const categoryError = ref('')
const records = ref<Topic[]>([])
const total = ref(0)
const categories = ref<Category[]>([])
const previewVisible = ref(false)
const previewTopic = ref<Topic | null>(null)
const previewContents = ref<TopicContentItem[]>([])
const previewLoading = ref(false)

const query = reactive<TopicQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: '',
  status: '',
})

const options = computed(() => categoryOptions(categories.value))
const permissions = computed(() => authStore.permissions)
const busy = computed(
  () =>
    loading.value
    || publishingId.value !== ''
    || offliningId.value !== ''
    || deletingId.value !== '',
)
const showCreate = computed(() => permissions.value.includes('TOPIC_CREATE'))
const emptyText = computed(() => (loadError.value ? '加载失败' : '暂无专题'))

function actions(row: Topic) {
  return visibleActions(row.status, permissions.value)
}

async function loadCategories() {
  categoryError.value = ''
  try {
    const response = await categoryApi.getCategoryTree()
    categories.value = normalizeTree(response.data.data ?? [])
  } catch (error) {
    categories.value = []
    categoryError.value = error instanceof Error ? error.message : '分类加载失败'
  }
}

async function loadList() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await topicApi.getTopicList(query)
    const page = response.data.data
    records.value = page?.records ?? []
    total.value = page?.total ?? 0
  } catch (error) {
    records.value = []
    total.value = 0
    loadError.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function onSearch() {
  Object.assign(query, searchQuery(query))
  void loadList()
}

function onReset() {
  query.keyword = ''
  query.categoryId = ''
  query.status = ''
  query.pageNum = 1
  query.pageSize = 10
  void loadList()
}

function onPageChange(pageNum: number) {
  query.pageNum = pageNum
  void loadList()
}

function onSizeChange(pageSize: number) {
  query.pageSize = pageSize
  query.pageNum = 1
  void loadList()
}

function openEditor(row: Topic) {
  router.push(`/topics/${row.id}/edit`)
}

async function openPreview(row: Topic) {
  previewTopic.value = row
  previewContents.value = []
  previewVisible.value = true
  if (!permissions.value.includes('TOPIC_CONTENT_MANAGE')) {
    return
  }
  previewLoading.value = true
  try {
    const response = await topicApi.getTopicContents(row.id)
    previewContents.value = response.data.data ?? []
  } catch {
    previewContents.value = []
  } finally {
    previewLoading.value = false
  }
}

async function onDelete(row: Topic) {
  try {
    await ElMessageBox.confirm(deleteConfirmText(row.name), '删除专题', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  if (deletingId.value) {
    return
  }
  deletingId.value = row.id
  const totalBefore = total.value
  try {
    await topicApi.deleteTopic(row.id)
    ElMessage.success('删除成功')
    query.pageNum = nextPageAfterDelete(query.pageNum, query.pageSize, totalBefore, 1)
    await loadList()
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    deletingId.value = ''
  }
}

async function onPublish(row: Topic) {
  try {
    await ElMessageBox.confirm(publishConfirmText(row.name), '发布专题', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  if (publishingId.value) {
    return
  }
  publishingId.value = row.id
  try {
    await topicApi.publishTopic(row.id)
    ElMessage.success('发布成功')
    await loadList()
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    publishingId.value = ''
  }
}

async function onOffline(row: Topic) {
  try {
    await ElMessageBox.confirm(offlineConfirmText(row.name), '下架专题', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  if (offliningId.value) {
    return
  }
  offliningId.value = row.id
  try {
    await topicApi.offlineTopic(row.id)
    ElMessage.success('下线成功')
    await loadList()
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    offliningId.value = ''
  }
}

onMounted(() => {
  void loadCategories()
  void loadList()
})
</script>

<template>
  <section class="topic-page">
    <div class="toolbar">
      <el-button v-if="showCreate" type="primary" :disabled="busy" @click="router.push('/topics/create')">
        新建专题
      </el-button>
    </div>
    <el-form class="filters" inline @submit.prevent="onSearch">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="搜索名称/编码" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="query.categoryId" clearable placeholder="全部分类" style="width: 220px">
          <el-option label="全部分类" value="" />
          <el-option v-for="item in options" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已下架" value="OFFLINE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :disabled="loading" @click="onSearch">搜索</el-button>
        <el-button :disabled="loading" @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="loadError" class="page-alert" type="error" :closable="false" show-icon :title="loadError">
      <el-button link type="primary" @click="loadList">重新加载</el-button>
    </el-alert>
    <el-alert v-if="categoryError" class="page-alert" type="error" :closable="false" show-icon :title="categoryError" />
    <el-table v-loading="loading" :data="records" border :empty-text="emptyText">
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="code" label="编码" min-width="120" />
      <el-table-column label="分类" min-width="140">
        <template #default="{ row }">{{ row.categoryName || '' }}</template>
      </el-table-column>
      <el-table-column prop="contentCount" label="内容数" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="发布时间" min-width="170">
        <template #default="{ row }">{{ formatTime(row.publishTime) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="380" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="actions(row).preview"
            link
            type="primary"
            :disabled="busy"
            @click="openPreview(row)"
          >
            预览
          </el-button>
          <el-button
            v-if="actions(row).edit"
            link
            type="primary"
            :disabled="busy"
            @click="openEditor(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="actions(row).manageContents"
            link
            type="primary"
            :disabled="busy"
            @click="openEditor(row)"
          >
            管理内容
          </el-button>
          <el-button
            v-if="actions(row).publish"
            link
            type="primary"
            :disabled="busy"
            :loading="publishingId === row.id"
            @click="onPublish(row)"
          >
            发布
          </el-button>
          <el-button
            v-if="actions(row).offline"
            link
            type="primary"
            :disabled="busy"
            :loading="offliningId === row.id"
            @click="onOffline(row)"
          >
            下线
          </el-button>
          <el-button
            v-if="actions(row).remove"
            link
            type="danger"
            :disabled="busy"
            :loading="deletingId === row.id"
            @click="onDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog v-model="previewVisible" title="专题预览" width="720px">
      <div v-if="previewTopic" v-loading="previewLoading" class="preview">
        <h2>{{ previewTopic.name }}</h2>
        <p>编码：{{ previewTopic.code }}</p>
        <p>分类：{{ previewTopic.categoryName || '' }}</p>
        <p>状态：{{ statusLabel(previewTopic.status) }}</p>
        <p v-if="previewTopic.summary">简介：{{ previewTopic.summary }}</p>
        <h3>关联内容</h3>
        <el-table v-if="previewContents.length" :data="previewContents" border size="small">
          <el-table-column prop="title" label="标题" min-width="160" />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">{{ contentTypeLabel(row.contentType) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">{{ statusLabel(row.status) }}</template>
          </el-table-column>
        </el-table>
        <p v-else class="preview-empty">暂无关联内容</p>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.toolbar,
.filters,
.page-alert {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.preview h2 {
  margin: 0 0 12px;
  font-size: 20px;
}

.preview h3 {
  margin: 16px 0 8px;
  font-size: 16px;
}

.preview p {
  margin: 0 0 8px;
  color: #646a73;
}

.preview-empty {
  color: #909399;
}
</style>

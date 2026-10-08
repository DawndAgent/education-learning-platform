<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ContentPreviewDialog from '@/components/ContentPreviewDialog.vue'
import { categoryApi } from '@/api/category'
import { contentApi } from '@/api/content'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types/category'
import type { Content, ContentQuery, ContentType } from '@/types/content'
import { normalizeTree } from '@/utils/category-form'
import {
  batchDeleteConfirmText,
  batchOfflineConfirmText,
  canDuplicate,
  canOpenEditor,
  categoryOptions,
  contentTypeLabel,
  deleteConfirmText,
  displayStatusLabel,
  editorPath,
  formatTime,
  nextPageAfterDelete,
  offlineConfirmText,
  publishConfirmText,
  publishTimeRange,
  searchQuery,
  selectableForBatchDelete,
  selectableForBatchOffline,
  statusTagType,
  visibleActions,
} from '@/utils/content-form'

const props = defineProps<{
  fixedType?: ContentType
}>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const publishingId = ref('')
const offliningId = ref('')
const deletingId = ref('')
const copyingId = ref('')
const batching = ref(false)
const loadError = ref('')
const categoryError = ref('')
const records = ref<Content[]>([])
const total = ref(0)
const categories = ref<Category[]>([])
const selectedRows = ref<Content[]>([])
const previewVisible = ref(false)
const previewId = ref('')
const previewType = ref<ContentType>('ARTICLE')

const query = reactive<ContentQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: '',
  contentType: '',
  status: '',
  schedule: '',
  publishTimeFrom: '',
  publishTimeTo: '',
})

const listState = ref<'' | 'DRAFT' | 'SCHEDULED' | 'PUBLISHED' | 'OFFLINE'>('')
const scheduleFilter = ref<'' | 'PLAIN' | 'SCHEDULED'>('')
const timePreset = ref<'' | 'today' | '7d' | '30d' | 'custom'>('')
const customRange = ref<[string, string] | null>(null)

const options = computed(() => categoryOptions(categories.value))
const permissions = computed(() => authStore.permissions)
const busy = computed(
  () =>
    loading.value
    || publishingId.value !== ''
    || offliningId.value !== ''
    || deletingId.value !== ''
    || copyingId.value !== ''
    || batching.value,
)
const showArticleCreate = computed(() => (!props.fixedType || props.fixedType === 'ARTICLE') && permissions.value.includes('CONTENT_CREATE'))
const showVideoCreate = computed(() => (!props.fixedType || props.fixedType === 'VIDEO') && permissions.value.includes('VIDEO_MANAGE'))
const showQuestionCreate = computed(() => (!props.fixedType || props.fixedType === 'QUESTION') && permissions.value.includes('CONTENT_CREATE'))
const showWeeklyCreate = computed(() => (!props.fixedType || props.fixedType === 'WEEKLY') && permissions.value.includes('CONTENT_CREATE'))
const showDocumentCreate = computed(() => (!props.fixedType || props.fixedType === 'DOCUMENT') && permissions.value.includes('CONTENT_CREATE'))
const showBatchPublish = computed(() => permissions.value.includes('CONTENT_PUBLISH'))
const showBatchOffline = computed(() => permissions.value.includes('CONTENT_OFFLINE'))
const showBatchDelete = computed(() => permissions.value.includes('CONTENT_DELETE'))
const emptyText = computed(() => (loadError.value ? '加载失败' : '暂无内容'))
const selectedCount = computed(() => selectedRows.value.length)
const canBatchOffline = computed(() => selectableForBatchOffline(selectedRows.value))
const canBatchDelete = computed(() => selectableForBatchDelete(selectedRows.value))

function actions(row: Content) {
  return visibleActions(row.status, permissions.value, row.contentType)
}

function onSelectionChange(rows: Content[]) {
  selectedRows.value = rows
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
    const response = await contentApi.getContentList(query)
    const page = response.data.data
    records.value = page?.records ?? []
    total.value = page?.total ?? 0
    selectedRows.value = []
  } catch (error) {
    records.value = []
    total.value = 0
    selectedRows.value = []
    loadError.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  if (listState.value === 'SCHEDULED') {
    query.status = 'DRAFT'
    query.schedule = 'SCHEDULED'
  } else if (listState.value === 'DRAFT') {
    query.status = 'DRAFT'
    query.schedule = scheduleFilter.value === 'SCHEDULED' ? 'SCHEDULED' : 'PLAIN'
  } else if (listState.value === 'PUBLISHED' || listState.value === 'OFFLINE') {
    query.status = listState.value
    query.schedule = scheduleFilter.value
  } else {
    query.status = ''
    query.schedule = scheduleFilter.value
  }
  if (timePreset.value === 'custom') {
    query.publishTimeFrom = customRange.value?.[0] ?? ''
    query.publishTimeTo = customRange.value?.[1] ?? ''
  } else if (timePreset.value === 'today' || timePreset.value === '7d' || timePreset.value === '30d') {
    const range = publishTimeRange(timePreset.value)
    query.publishTimeFrom = range.from
    query.publishTimeTo = range.to
  } else {
    query.publishTimeFrom = ''
    query.publishTimeTo = ''
  }
}

function onSearch() {
  applyFilters()
  Object.assign(query, searchQuery(query))
  void loadList()
}

function onReset() {
  query.keyword = ''
  query.categoryId = ''
  query.contentType = props.fixedType ?? ''
  query.status = ''
  query.schedule = ''
  query.publishTimeFrom = ''
  query.publishTimeTo = ''
  listState.value = ''
  scheduleFilter.value = ''
  timePreset.value = ''
  customRange.value = null
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

function openEditor(row: Content) {
  router.push(editorPath(row.contentType, row.id))
}

function openPreview(row: Content) {
  previewId.value = row.id
  previewType.value = row.contentType
  previewVisible.value = true
}

async function onDelete(row: Content) {
  try {
    await ElMessageBox.confirm(deleteConfirmText(row.title), '删除内容', {
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
    await contentApi.deleteContent(row.id)
    ElMessage.success('删除成功')
    query.pageNum = nextPageAfterDelete(query.pageNum, query.pageSize, totalBefore, 1)
    await loadList()
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    deletingId.value = ''
  }
}

async function onPublish(row: Content) {
  try {
    await ElMessageBox.confirm(
      publishConfirmText(row.title, contentTypeLabel(row.contentType), row.categoryName || '未分类'),
      '发布内容',
      {
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
    await contentApi.publishContent(row.id)
    ElMessage.success('发布成功')
    await loadList()
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    publishingId.value = ''
  }
}

async function onOffline(row: Content) {
  try {
    await ElMessageBox.confirm(offlineConfirmText(row.title), '下架内容', {
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
    await contentApi.offlineContent(row.id)
    ElMessage.success('下线成功')
    await loadList()
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    offliningId.value = ''
  }
}

async function onDuplicate(row: Content) {
  if (!canDuplicate(row.contentType, permissions.value) || copyingId.value) {
    return
  }
  copyingId.value = row.id
  try {
    const response = await contentApi.duplicateContent(row.id)
    const created = response.data.data
    ElMessage.success('已复制为草稿')
    await router.push(editorPath(created.contentType, created.id))
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    copyingId.value = ''
  }
}

async function onBatchPublish() {
  if (!showBatchPublish.value || selectedRows.value.length === 0 || batching.value) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定立即发布选中的 ${selectedRows.value.length} 条内容吗？不符合发布条件的内容会保持原状态。`, '批量发布', {
      type: 'warning',
      confirmButtonText: '发布',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  batching.value = true
  try {
    const result = await contentApi.batchPublishContents(selectedRows.value.map((row) => row.id))
    const data = result.data.data
    const text = `成功 ${data.successCount}，失败 ${data.failedCount}`
    if (data.failedCount > 0) {
      ElMessage.warning(text)
    } else {
      ElMessage.success(text)
    }
    await loadList()
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    batching.value = false
  }
}

async function onBatchOffline() {
  if (!canBatchOffline.value || batching.value) {
    return
  }
  const count = selectedRows.value.length
  try {
    await ElMessageBox.confirm(batchOfflineConfirmText(count), '批量下线', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  batching.value = true
  try {
    const result = await contentApi.batchOfflineContents(selectedRows.value.map((row) => row.id))
    ElMessage.success(`下线成功 ${result.data.data.successCount} 条`)
    await loadList()
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    batching.value = false
  }
}

async function onBatchDelete() {
  if (!canBatchDelete.value || batching.value) {
    return
  }
  const count = selectedRows.value.length
  try {
    await ElMessageBox.confirm(batchDeleteConfirmText(count), '批量删除', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  batching.value = true
  const totalBefore = total.value
  try {
    const result = await contentApi.batchDeleteContents(selectedRows.value.map((row) => row.id))
    const deleted = result.data.data.successCount
    ElMessage.success(`删除成功 ${deleted} 条`)
    query.pageNum = nextPageAfterDelete(query.pageNum, query.pageSize, totalBefore, deleted)
    await loadList()
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    batching.value = false
  }
}

watch(
  () => props.fixedType,
  () => {
    query.contentType = props.fixedType ?? ''
    query.pageNum = 1
    void loadList()
  },
)

onMounted(() => {
  if (props.fixedType) {
    query.contentType = props.fixedType
  }
  if (route.query.schedule === 'SCHEDULED') {
    listState.value = 'SCHEDULED'
    scheduleFilter.value = 'SCHEDULED'
    applyFilters()
  }
  void loadCategories()
  void loadList()
})
</script>

<template>
  <section class="content-page">
    <div class="toolbar">
      <el-button v-if="showArticleCreate" type="primary" :disabled="busy" @click="router.push('/articles/create')">
        新增文章
      </el-button>
      <el-button v-if="showVideoCreate" type="primary" :disabled="busy" @click="router.push('/videos/create')">
        新增视频
      </el-button>
      <el-button v-if="showQuestionCreate" type="primary" :disabled="busy" @click="router.push('/questions/create')">
        新增题目
      </el-button>
      <el-button v-if="showQuestionCreate" :disabled="busy" @click="router.push('/questions/import-pdf')">
        从 PDF 导入
      </el-button>
      <el-button v-if="showWeeklyCreate" type="primary" :disabled="busy" @click="router.push('/weeklies/create')">
        新增每周一题
      </el-button>
      <el-button v-if="showDocumentCreate" type="primary" :disabled="busy" @click="router.push('/documents/create')">
        新增资料
      </el-button>
      <el-button
        v-if="showBatchPublish"
        :disabled="busy || selectedCount === 0"
        :loading="batching"
        @click="onBatchPublish"
      >
        批量立即发布
      </el-button>
      <el-button
        v-if="showBatchOffline"
        :disabled="busy || !canBatchOffline"
        :loading="batching"
        @click="onBatchOffline"
      >
        批量下线
      </el-button>
      <el-button
        v-if="showBatchDelete"
        type="danger"
        plain
        :disabled="busy || !canBatchDelete"
        :loading="batching"
        @click="onBatchDelete"
      >
        批量删除
      </el-button>
      <span v-if="selectedCount > 0" class="selection-hint">已选择 {{ selectedCount }} 条</span>
    </div>
    <el-form class="filters" inline @submit.prevent="onSearch">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="搜索标题" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="query.categoryId" clearable placeholder="全部分类" style="width: 220px">
          <el-option label="全部分类" value="" />
          <el-option v-for="item in options" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!fixedType" label="类型">
        <el-select v-model="query.contentType" clearable placeholder="全部" style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="文章" value="ARTICLE" />
          <el-option label="视频" value="VIDEO" />
          <el-option label="题目" value="QUESTION" />
          <el-option label="每周一题" value="WEEKLY" />
          <el-option label="资料" value="DOCUMENT" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="listState" clearable placeholder="全部" style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="待定时发布" value="SCHEDULED" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已下线" value="OFFLINE" />
        </el-select>
      </el-form-item>
      <el-form-item label="定时">
        <el-select v-model="scheduleFilter" clearable placeholder="全部" style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="普通草稿" value="PLAIN" />
          <el-option label="待定时发布" value="SCHEDULED" />
        </el-select>
      </el-form-item>
      <el-form-item label="发布时间">
        <el-select v-model="timePreset" clearable placeholder="不限" style="width: 140px">
          <el-option label="不限" value="" />
          <el-option label="今天" value="today" />
          <el-option label="最近 7 天" value="7d" />
          <el-option label="最近 30 天" value="30d" />
          <el-option label="自定义" value="custom" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="timePreset === 'custom'">
        <el-date-picker
          v-model="customRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
        />
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
    <el-table
      v-loading="loading"
      :data="records"
      border
      :empty-text="emptyText"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ contentTypeLabel(row.contentType) }}</template>
      </el-table-column>
      <el-table-column label="分类" min-width="140">
        <template #default="{ row }">{{ row.categoryName || '' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ displayStatusLabel(row.status, row.scheduledPublishTime) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="发布时间" min-width="170">
        <template #default="{ row }">{{ formatTime(row.publishTime) }}</template>
      </el-table-column>
      <el-table-column label="计划发布时间" min-width="170">
        <template #default="{ row }">{{ formatTime(row.scheduledPublishTime) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" min-width="170">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
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
            v-if="canOpenEditor(row.contentType, permissions)"
            link
            type="primary"
            :disabled="busy"
            @click="openEditor(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="actions(row).copy"
            link
            type="primary"
            :disabled="busy"
            :loading="copyingId === row.id"
            @click="onDuplicate(row)"
          >
            复制
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

    <ContentPreviewDialog
      v-model:visible="previewVisible"
      :content-id="previewId"
      :content-type="previewType"
    />
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

.selection-hint {
  margin-left: 8px;
  color: #646a73;
  font-size: 13px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

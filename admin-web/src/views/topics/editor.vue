<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { categoryApi } from '@/api/category'
import { contentApi } from '@/api/content'
import { topicApi } from '@/api/topic'
import ImageUpload from '@/components/ImageUpload.vue'
import { useUnsavedLeave } from '@/composables/useUnsavedLeave'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types/category'
import type { Content } from '@/types/content'
import type { TopicContentItem, TopicStatus } from '@/types/topic'
import { normalizeTree } from '@/utils/category-form'
import {
  categoryOptions,
  contentTypeLabel,
  statusLabel,
} from '@/utils/content-form'
import {
  moveContentItem,
  offlineConfirmText,
  publishActionLabel,
  publishConfirmText,
  removeContentConfirmText,
  saveSuccessMessage,
  toCreatePayload,
  toSortPayload,
  toUpdatePayload,
  validateTopicForm,
  type TopicFormValues,
} from '@/utils/topic-form'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const offlining = ref(false)
const contentsLoading = ref(false)
const addingContents = ref(false)
const sortingContents = ref(false)
const removingContentId = ref('')
const loadError = ref('')
const previewVisible = ref(false)
const addDialogVisible = ref(false)
const categories = ref<Category[]>([])
const contents = ref<TopicContentItem[]>([])
const formRef = ref<FormInstance>()
const topicId = ref(route.name === 'topic-create' ? '' : String(route.params.id || ''))
const status = ref<TopicStatus>('DRAFT')
const snapshot = ref('')

const form = reactive<TopicFormValues>({
  name: '',
  code: '',
  coverUrl: '',
  summary: '',
  categoryId: '',
  sort: 0,
})

const rules: FormRules = {
  name: [{ required: true, message: '专题名称不能为空', trigger: 'blur' }],
  code: [{ required: true, message: '专题编码不能为空', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'change' }],
}

const contentPickerQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: '',
  contentType: '' as '' | 'ARTICLE' | 'VIDEO' | 'QUESTION' | 'WEEKLY' | 'DOCUMENT',
  status: '' as '' | 'DRAFT' | 'PUBLISHED' | 'OFFLINE',
})
const contentPickerRecords = ref<Content[]>([])
const contentPickerTotal = ref(0)
const contentPickerLoading = ref(false)
const selectedContentIds = ref<string[]>([])

const creating = computed(() => topicId.value === '')
const options = computed(() => categoryOptions(categories.value))
const leafIds = computed(() => options.value.filter((item) => item.leaf).map((item) => item.id))
const formOptions = computed(() => options.value.filter((item) => item.leaf || item.id === form.categoryId))
const canSave = computed(() =>
  authStore.permissions.includes(creating.value ? 'TOPIC_CREATE' : 'TOPIC_UPDATE'),
)
const canPublish = computed(() => status.value !== 'PUBLISHED' && authStore.permissions.includes('TOPIC_PUBLISH'))
const canOffline = computed(() => status.value === 'PUBLISHED' && authStore.permissions.includes('TOPIC_OFFLINE'))
const canManageContents = computed(() => authStore.permissions.includes('TOPIC_CONTENT_MANAGE'))
const busy = computed(
  () =>
    saving.value
    || publishing.value
    || offlining.value
    || addingContents.value
    || sortingContents.value
    || removingContentId.value !== '',
)
const dirty = computed(() => snapshot.value !== '' && snapshot.value !== JSON.stringify(form))
const linkedIds = computed(() => new Set(contents.value.map((item) => item.contentId)))

useUnsavedLeave(dirty)

function capture() {
  snapshot.value = JSON.stringify(form)
}

async function loadCategories() {
  const response = await categoryApi.getCategoryTree()
  categories.value = normalizeTree(response.data.data ?? [])
}

async function loadContents() {
  if (!topicId.value || !canManageContents.value) {
    contents.value = []
    return
  }
  contentsLoading.value = true
  try {
    const response = await topicApi.getTopicContents(topicId.value)
    contents.value = response.data.data ?? []
  } catch {
    contents.value = []
  } finally {
    contentsLoading.value = false
  }
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    if (creating.value) {
      await loadCategories()
      contents.value = []
      capture()
      return
    }
    const [categoryResponse, topicResponse] = await Promise.all([
      categoryApi.getCategoryTree(),
      topicApi.getTopicDetail(topicId.value),
    ])
    categories.value = normalizeTree(categoryResponse.data.data ?? [])
    const topic = topicResponse.data.data
    form.name = topic.name
    form.code = topic.code
    form.coverUrl = topic.coverUrl ?? ''
    form.summary = topic.summary ?? ''
    form.categoryId = topic.categoryId
    form.sort = topic.sort
    status.value = topic.status
    capture()
    await loadContents()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '获取专题失败'
  } finally {
    loading.value = false
  }
}

async function saveDraft() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return false
  }
  const message = validateTopicForm(form, leafIds.value, creating.value)
  if (message) {
    ElMessage.error(message)
    return false
  }
  saving.value = true
  try {
    const response = creating.value
      ? await topicApi.createTopic(toCreatePayload(form))
      : await topicApi.updateTopic(topicId.value, toUpdatePayload(form))
    const topic = response.data.data
    topicId.value = topic.id
    status.value = topic.status
    form.code = topic.code
    capture()
    if (route.name === 'topic-create') {
      await router.replace(`/topics/${topic.id}/edit`)
    }
    return true
  } catch {
    return false
  } finally {
    saving.value = false
  }
}

async function onSave() {
  if (!canSave.value || busy.value) {
    return
  }
  const saved = await saveDraft()
  if (saved) {
    ElMessage.success(saveSuccessMessage(status.value))
  }
}

async function onPublish() {
  if (!canPublish.value || busy.value) {
    return
  }
  const message = validateTopicForm(form, leafIds.value, creating.value)
  if (message) {
    ElMessage.error(message)
    return
  }
  const wasOffline = status.value === 'OFFLINE'
  try {
    await ElMessageBox.confirm(publishConfirmText(form.name.trim()), wasOffline ? '重新发布' : '发布', {
      type: 'warning',
      confirmButtonText: wasOffline ? '重新发布' : '发布',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  const saved = await saveDraft()
  if (!saved || !topicId.value) {
    return
  }
  publishing.value = true
  try {
    const response = await topicApi.publishTopic(topicId.value)
    status.value = response.data.data.status
    capture()
    ElMessage.success(wasOffline ? '重新发布成功' : '发布成功')
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    publishing.value = false
  }
}

async function onOffline() {
  if (!canOffline.value || busy.value || !topicId.value) {
    return
  }
  try {
    await ElMessageBox.confirm(offlineConfirmText(form.name.trim()), '下线', {
      type: 'warning',
      confirmButtonText: '下线',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  offlining.value = true
  try {
    const response = await topicApi.offlineTopic(topicId.value)
    status.value = response.data.data.status
    ElMessage.success('下线成功')
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    offlining.value = false
  }
}

function onMove(index: number, direction: -1 | 1) {
  contents.value = moveContentItem(contents.value, index, direction)
}

async function onSaveSort() {
  if (!canManageContents.value || !topicId.value || sortingContents.value) {
    return
  }
  sortingContents.value = true
  try {
    const response = await topicApi.sortTopicContents(topicId.value, {
      items: toSortPayload(contents.value),
    })
    contents.value = response.data.data ?? contents.value
    ElMessage.success('排序已保存')
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    sortingContents.value = false
  }
}

async function onRemoveContent(row: TopicContentItem) {
  if (!canManageContents.value || !topicId.value || removingContentId.value) {
    return
  }
  try {
    await ElMessageBox.confirm(removeContentConfirmText(row.title), '移除内容', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  removingContentId.value = row.contentId
  try {
    await topicApi.removeTopicContent(topicId.value, row.contentId)
    ElMessage.success('已移除')
    await loadContents()
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    removingContentId.value = ''
  }
}

async function openAddDialog() {
  if (!canManageContents.value || !topicId.value || busy.value) {
    return
  }
  contentPickerQuery.pageNum = 1
  contentPickerQuery.keyword = ''
  contentPickerQuery.categoryId = ''
  contentPickerQuery.contentType = ''
  contentPickerQuery.status = ''
  selectedContentIds.value = []
  addDialogVisible.value = true
  await loadContentPicker()
}

async function loadContentPicker() {
  contentPickerLoading.value = true
  try {
    const response = await contentApi.getContentList(contentPickerQuery)
    const page = response.data.data
    contentPickerRecords.value = page?.records ?? []
    contentPickerTotal.value = page?.total ?? 0
  } catch {
    contentPickerRecords.value = []
    contentPickerTotal.value = 0
  } finally {
    contentPickerLoading.value = false
  }
}

function onContentSelectionChange(rows: Content[]) {
  selectedContentIds.value = rows
    .map((row) => row.id)
    .filter((id) => !linkedIds.value.has(id))
}

function isContentSelectable(row: Content) {
  return !linkedIds.value.has(row.id)
}

function onContentPickerPageChange(page: number) {
  contentPickerQuery.pageNum = page
  void loadContentPicker()
}

async function onConfirmAddContents() {
  if (!topicId.value || addingContents.value || selectedContentIds.value.length === 0) {
    return
  }
  addingContents.value = true
  try {
    const response = await topicApi.addTopicItems(topicId.value, selectedContentIds.value)
    const result = response.data.data
    const listed = await topicApi.getTopicContents(topicId.value)
    contents.value = listed.data.data ?? contents.value
    ElMessage.success(
      `添加完成：成功 ${result?.successCount ?? 0}，重复 ${result?.duplicateCount ?? 0}，无效 ${result?.invalidCount ?? 0}`,
    )
    addDialogVisible.value = false
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    addingContents.value = false
  }
}

function openPreview() {
  previewVisible.value = true
}

watch(
  () => [route.name, route.params.id] as const,
  () => {
    const next = route.name === 'topic-create' ? '' : String(route.params.id || '')
    if (next === topicId.value) {
      return
    }
    topicId.value = next
    void load()
  },
)

onMounted(() => {
  void load()
})
</script>

<template>
  <section v-loading="loading">
    <div class="toolbar">
      <el-button @click="router.push('/topics')">返回专题列表</el-button>
      <div class="toolbar-actions">
        <el-button :loading="saving" :disabled="!canSave || busy" @click="onSave">保存</el-button>
        <el-button :disabled="busy" @click="openPreview">预览</el-button>
        <el-button v-if="canPublish" type="primary" :loading="publishing" :disabled="busy" @click="onPublish">
          {{ publishActionLabel(status) }}
        </el-button>
        <el-button v-if="canOffline" :loading="offlining" :disabled="busy" @click="onOffline">下线</el-button>
      </div>
    </div>
    <el-alert v-if="loadError" type="error" :closable="false" show-icon :title="loadError">
      <el-button link type="primary" @click="load">重新加载</el-button>
    </el-alert>
    <template v-else>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" maxlength="100" show-word-limit :disabled="!creating" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择二级分类">
            <el-option
              v-for="item in formOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
              :disabled="!item.leaf"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-tag>{{ statusLabel(status) }}</el-tag>
        </el-form-item>
        <el-form-item label="封面">
          <ImageUpload v-model="form.coverUrl" scene="COVER" :disabled="!canSave" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.summary" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>

      <div v-if="!creating && canManageContents" class="contents-section">
        <div class="contents-toolbar">
          <h3>专题内容</h3>
          <div class="toolbar-actions">
            <el-button :disabled="busy" @click="openAddDialog">添加内容</el-button>
            <el-button :loading="sortingContents" :disabled="busy || contents.length === 0" @click="onSaveSort">
              保存排序
            </el-button>
          </div>
        </div>
        <el-table v-loading="contentsLoading" :data="contents" border empty-text="暂无关联内容">
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">{{ contentTypeLabel(row.contentType) }}</template>
          </el-table-column>
          <el-table-column label="分类" min-width="140">
            <template #default="{ row }">{{ row.categoryName || '' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">{{ statusLabel(row.status) }}</template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="80" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row, $index }">
              <el-button link type="primary" :disabled="busy || $index === 0" @click="onMove($index, -1)">
                上移
              </el-button>
              <el-button
                link
                type="primary"
                :disabled="busy || $index === contents.length - 1"
                @click="onMove($index, 1)"
              >
                下移
              </el-button>
              <el-button
                link
                type="danger"
                :disabled="busy"
                :loading="removingContentId === row.contentId"
                @click="onRemoveContent(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-alert
        v-else-if="creating"
        class="contents-hint"
        type="info"
        :closable="false"
        title="保存专题后可管理关联内容"
      />
    </template>

    <el-dialog v-model="addDialogVisible" title="添加内容" width="800px">
      <el-form inline @submit.prevent="loadContentPicker">
        <el-form-item label="关键词">
          <el-input v-model="contentPickerQuery.keyword" clearable placeholder="搜索标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="contentPickerQuery.contentType" clearable placeholder="全部" style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="文章" value="ARTICLE" />
            <el-option label="视频" value="VIDEO" />
            <el-option label="题目" value="QUESTION" />
            <el-option label="每周一题" value="WEEKLY" />
            <el-option label="资料" value="DOCUMENT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="contentPickerLoading" @click="loadContentPicker">搜索</el-button>
        </el-form-item>
      </el-form>
      <el-table
        v-loading="contentPickerLoading"
        :data="contentPickerRecords"
        border
        @selection-change="onContentSelectionChange"
      >
        <el-table-column type="selection" width="48" :selectable="isContentSelectable" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ contentTypeLabel(row.contentType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          :current-page="contentPickerQuery.pageNum"
          :page-size="contentPickerQuery.pageSize"
          :total="contentPickerTotal"
          layout="total, prev, pager, next"
          background
          @current-change="onContentPickerPageChange"
        />
      </div>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="addingContents"
          :disabled="selectedContentIds.length === 0 || busy"
          @click="onConfirmAddContents"
        >
          添加选中
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="专题预览" width="720px">
      <article class="preview">
        <h1>{{ form.name }}</h1>
        <p>编码：{{ form.code }}</p>
        <p>状态：{{ statusLabel(status) }}</p>
        <p v-if="form.summary">简介：{{ form.summary }}</p>
        <h3>关联内容（{{ contents.length }}）</h3>
        <el-table v-if="contents.length" :data="contents" border size="small">
          <el-table-column prop="title" label="标题" min-width="160" />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">{{ contentTypeLabel(row.contentType) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">{{ statusLabel(row.status) }}</template>
          </el-table-column>
        </el-table>
        <p v-else class="preview-empty">暂无关联内容</p>
      </article>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.contents-section {
  margin-top: 24px;
}

.contents-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.contents-toolbar h3 {
  margin: 0;
  font-size: 16px;
}

.contents-hint {
  margin-top: 16px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.preview h1 {
  margin: 0 0 12px;
  font-size: 22px;
  line-height: 1.4;
}

.preview h3 {
  margin: 16px 0 8px;
  font-size: 16px;
}

.preview p {
  margin: 0 0 8px;
  color: #666;
}

.preview-empty {
  color: #909399;
}
</style>

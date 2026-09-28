<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { categoryApi } from '@/api/category'
import { contentApi } from '@/api/content'
import { documentApi } from '@/api/document'
import ContentScheduleActions from '@/components/ContentScheduleActions.vue'
import DocumentFileUpload from '@/components/DocumentFileUpload.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { useUnsavedLeave } from '@/composables/useUnsavedLeave'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types/category'
import type { ContentStatus } from '@/types/content'
import type { FileUploadResult } from '@/types/file'
import { normalizeTree } from '@/utils/category-form'
import { categoryOptions, displayStatusLabel, formatTime, offlineConfirmText, publishActionLabel, publishConfirmText, saveSuccessMessage } from '@/utils/content-form'
import { toDocumentPayload, validateDocumentForm, type DocumentFormValues } from '@/utils/document-form'
import { formatFileSize } from '@/utils/file-upload'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const offlining = ref(false)
const loadError = ref('')
const previewVisible = ref(false)
const categories = ref<Category[]>([])
const formRef = ref<FormInstance>()
const contentId = ref(route.name === 'document-create' ? '' : String(route.params.id || ''))
const status = ref<ContentStatus>('DRAFT')
const publishTime = ref<string | null>(null)
const scheduledPublishTime = ref<string | null>(null)
const snapshot = ref('')

const form = reactive<DocumentFormValues>({
  title: '',
  categoryId: '',
  coverUrl: '',
  summary: '',
  sort: 0,
  fileUrl: '',
  fileName: '',
  fileSize: null,
  fileType: '',
  downloadUrl: '',
  previewUrl: '',
  description: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'change' }],
}

const creating = computed(() => contentId.value === '')
const options = computed(() => categoryOptions(categories.value))
const leafIds = computed(() => options.value.filter((item) => item.leaf).map((item) => item.id))
const formOptions = computed(() => options.value.filter((item) => item.leaf || item.id === form.categoryId))
const canSave = computed(() => authStore.permissions.includes(creating.value ? 'CONTENT_CREATE' : 'CONTENT_UPDATE'))
const canPublish = computed(() => status.value !== 'PUBLISHED' && authStore.permissions.includes('CONTENT_PUBLISH'))
const canOffline = computed(() => status.value === 'PUBLISHED' && authStore.permissions.includes('CONTENT_OFFLINE'))
const busy = computed(() => saving.value || publishing.value || offlining.value)
const dirty = computed(() => snapshot.value !== '' && snapshot.value !== JSON.stringify(form))
const sizeText = computed(() => formatFileSize(form.fileSize))

useUnsavedLeave(dirty)

function applyPublishState(value: { status: ContentStatus; scheduledPublishTime: string | null; publishTime: string | null }) {
  status.value = value.status
  scheduledPublishTime.value = value.scheduledPublishTime
  publishTime.value = value.publishTime
}

function capture() {
  snapshot.value = JSON.stringify(form)
}

function applyUploadResult(result: FileUploadResult) {
  form.fileUrl = result.url
  form.downloadUrl = result.url
  form.previewUrl = result.url
  form.fileName = result.fileName
  form.fileSize = result.size
  form.fileType = result.contentType
}

function clearFile() {
  form.fileUrl = ''
  form.downloadUrl = ''
  form.previewUrl = ''
  form.fileName = ''
  form.fileSize = null
  form.fileType = ''
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    if (creating.value) {
      const response = await categoryApi.getCategoryTree()
      categories.value = normalizeTree(response.data.data ?? [])
      capture()
      return
    }
    const [categoryResponse, documentResponse] = await Promise.all([
      categoryApi.getCategoryTree(),
      documentApi.getDocument(contentId.value),
    ])
    categories.value = normalizeTree(categoryResponse.data.data ?? [])
    const document = documentResponse.data.data
    form.title = document.title
    form.categoryId = String(document.categoryId)
    form.coverUrl = document.coverUrl ?? ''
    form.summary = document.summary ?? ''
    form.sort = document.sort
    form.fileUrl = document.fileUrl ?? ''
    form.fileName = document.fileName ?? ''
    form.fileSize = document.fileSize
    form.fileType = document.fileType ?? ''
    form.downloadUrl = document.downloadUrl ?? ''
    form.previewUrl = document.previewUrl ?? ''
    form.description = document.description ?? ''
    status.value = document.status
    publishTime.value = document.publishTime
    scheduledPublishTime.value = document.scheduledPublishTime
    capture()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '获取资料失败'
  } finally {
    loading.value = false
  }
}

async function saveDraft() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return false
  }
  const message = validateDocumentForm(form, leafIds.value)
  if (message) {
    ElMessage.error(message)
    return false
  }
  saving.value = true
  try {
    const payload = toDocumentPayload(form)
    const response = creating.value
      ? await documentApi.createDocument(payload)
      : await documentApi.updateDocument(contentId.value, payload)
    const document = response.data.data
    contentId.value = String(document.contentId)
    status.value = document.status
    publishTime.value = document.publishTime
    scheduledPublishTime.value = document.scheduledPublishTime
    capture()
    if (route.name === 'document-create') {
      await router.replace(`/documents/${document.contentId}`)
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
  const message = validateDocumentForm(form, leafIds.value)
  if (message) {
    ElMessage.error(message)
    return
  }
  const wasOffline = status.value === 'OFFLINE'
  try {
    await ElMessageBox.confirm(publishConfirmText(form.title.trim()), wasOffline ? '重新发布' : '发布', {
      type: 'warning',
      confirmButtonText: wasOffline ? '重新发布' : '发布',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  const saved = await saveDraft()
  if (!saved || !contentId.value) {
    return
  }
  publishing.value = true
  try {
    const response = await contentApi.publishContent(contentId.value)
    status.value = response.data.data.status
    publishTime.value = response.data.data.publishTime
    scheduledPublishTime.value = response.data.data.scheduledPublishTime
    capture()
    ElMessage.success(wasOffline ? '重新发布成功' : '发布成功')
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    publishing.value = false
  }
}

async function onOffline() {
  if (!canOffline.value || busy.value || !contentId.value) {
    return
  }
  try {
    await ElMessageBox.confirm(offlineConfirmText(form.title.trim()), '下线', {
      type: 'warning',
      confirmButtonText: '下线',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  offlining.value = true
  try {
    const response = await contentApi.offlineContent(contentId.value)
    status.value = response.data.data.status
    publishTime.value = response.data.data.publishTime
    scheduledPublishTime.value = response.data.data.scheduledPublishTime
    ElMessage.success('下线成功')
  } catch {
    // 请求拦截器已用 ElMessage.error 展示后端错误
  } finally {
    offlining.value = false
  }
}

watch(
  () => [route.name, route.params.id] as const,
  () => {
    const next = route.name === 'document-create' ? '' : String(route.params.id || '')
    if (next === contentId.value) {
      return
    }
    contentId.value = next
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
      <el-button @click="router.push('/documents')">返回内容列表</el-button>
      <div class="toolbar-actions">
        <el-button :loading="saving" :disabled="!canSave || busy" @click="onSave">保存草稿</el-button>
        <el-button :disabled="busy" @click="previewVisible = true">预览</el-button>
        <el-button v-if="canPublish" type="primary" :loading="publishing" :disabled="busy" @click="onPublish">
          {{ publishActionLabel(status) }}
        </el-button>
        <ContentScheduleActions
          :content-id="contentId"
          :status="status"
          :scheduled-publish-time="scheduledPublishTime"
          :title="form.title"
          :disabled="busy"
          :can-publish="canPublish"
          @updated="applyPublishState"
        />
        <el-button v-if="canOffline" :loading="offlining" :disabled="busy" @click="onOffline">下线</el-button>
      </div>
    </div>
    <el-alert v-if="loadError" type="error" :closable="false" show-icon :title="loadError">
      <el-button link type="primary" @click="load">重新加载</el-button>
    </el-alert>
    <el-form v-else ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" maxlength="128" show-word-limit />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择二级分类">
          <el-option v-for="item in formOptions" :key="item.id" :label="item.label" :value="item.id" :disabled="!item.leaf" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-tag>{{ displayStatusLabel(status, scheduledPublishTime) }}</el-tag>
      </el-form-item>
      <el-form-item label="发布时间">
        <span>{{ formatTime(publishTime) || '—' }}</span>
      </el-form-item>
      <el-form-item v-if="scheduledPublishTime" label="计划发布时间">
        <span>{{ formatTime(scheduledPublishTime) }}</span>
      </el-form-item>
      <el-form-item label="类型">
        <el-input model-value="资料" disabled />
      </el-form-item>
      <el-form-item label="封面">
        <ImageUpload v-model="form.coverUrl" scene="COVER" :disabled="!canSave" />
      </el-form-item>
      <el-form-item label="摘要">
        <el-input v-model="form.summary" type="textarea" maxlength="512" show-word-limit />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="form.sort" :min="0" :max="9999" />
      </el-form-item>
      <el-form-item label="资料文件">
        <DocumentFileUpload
          :file-url="form.fileUrl"
          :file-name="form.fileName"
          :file-size="form.fileSize"
          :file-type="form.fileType"
          :disabled="!canSave"
          @success="applyUploadResult"
          @clear="clearFile"
        />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" maxlength="1000" show-word-limit />
      </el-form-item>
    </el-form>
    <el-dialog v-model="previewVisible" title="资料预览" width="640px">
      <div class="preview">
        <h1>{{ form.title }}</h1>
        <p v-if="form.summary">摘要：{{ form.summary }}</p>
        <p v-if="form.description">描述：{{ form.description }}</p>
        <p>文件名：{{ form.fileName || '—' }}</p>
        <p>类型：{{ form.fileType || '—' }}</p>
        <p>大小：{{ sizeText || '—' }}</p>
        <p v-if="form.fileUrl">文件地址：{{ form.fileUrl }}</p>
        <p v-if="form.downloadUrl">下载地址：{{ form.downloadUrl }}</p>
        <p v-if="form.previewUrl">预览地址：{{ form.previewUrl }}</p>
      </div>
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

.preview h1 {
  margin: 0 0 12px;
  font-size: 22px;
  line-height: 1.4;
}

.preview p {
  margin: 0 0 8px;
  color: #666;
  word-break: break-all;
}
</style>

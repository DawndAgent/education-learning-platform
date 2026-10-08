<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { articleApi } from '@/api/article'
import { categoryApi } from '@/api/category'
import { contentApi } from '@/api/content'
import RichTextEditor from '@/components/RichTextEditor.vue'
import ContentScheduleActions from '@/components/ContentScheduleActions.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import VideoMiniprogramQrDialog from '@/components/VideoMiniprogramQrDialog.vue'
import { useUnsavedLeave } from '@/composables/useUnsavedLeave'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types/category'
import type { ContentStatus } from '@/types/content'
import { normalizeTree } from '@/utils/category-form'
import { categoryOptions, displayStatusLabel, formatTime, offlineConfirmText, publishActionLabel, publishConfirmText, saveSuccessMessage } from '@/utils/content-form'
import {
  buildVideoMiniprogramQrHtml,
  previewHtml,
  toArticlePayload,
  validateArticleForm,
  type ArticleFormValues,
} from '@/utils/article-form'

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
const editorRef = ref<{ insertHtml: (html: string) => void }>()
const contentId = ref(route.name === 'article-create' ? '' : String(route.params.id || ''))
const status = ref<ContentStatus>('DRAFT')
const publishTime = ref<string | null>(null)
const scheduledPublishTime = ref<string | null>(null)
const snapshot = ref('')
const qrDialogVisible = ref(false)

const form = reactive<ArticleFormValues>({
  title: '',
  categoryId: '',
  coverUrl: '',
  summary: '',
  sort: 0,
  body: '<p><br></p>',
  author: '',
  source: '',
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
const safeBody = computed(() => previewHtml(form.body))

useUnsavedLeave(dirty)

function applyPublishState(value: { status: ContentStatus; scheduledPublishTime: string | null; publishTime: string | null }) {
  status.value = value.status
  scheduledPublishTime.value = value.scheduledPublishTime
  publishTime.value = value.publishTime
}

function capture() {
  snapshot.value = JSON.stringify(form)
}

async function loadCategories() {
  const response = await categoryApi.getCategoryTree()
  categories.value = normalizeTree(response.data.data ?? [])
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    if (creating.value) {
      await loadCategories()
      capture()
      return
    }
    const [categoryResponse, articleResponse] = await Promise.all([
      categoryApi.getCategoryTree(),
      articleApi.getArticle(contentId.value),
    ])
    categories.value = normalizeTree(categoryResponse.data.data ?? [])
    const article = articleResponse.data.data
    form.title = article.title
    form.categoryId = article.categoryId
    form.coverUrl = article.coverUrl ?? ''
    form.summary = article.summary ?? ''
    form.sort = article.sort
    form.body = article.body || '<p><br></p>'
    form.author = article.author ?? ''
    form.source = article.source ?? ''
    status.value = article.status
    publishTime.value = article.publishTime
    scheduledPublishTime.value = article.scheduledPublishTime
    capture()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '获取文章失败'
  } finally {
    loading.value = false
  }
}

async function saveDraft() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return false
  }
  const message = validateArticleForm(form, leafIds.value)
  if (message) {
    ElMessage.error(message)
    return false
  }
  saving.value = true
  try {
    const payload = toArticlePayload(form)
    const response = creating.value
      ? await articleApi.createArticle(payload)
      : await articleApi.updateArticle(contentId.value, payload)
    const article = response.data.data
    contentId.value = article.contentId
    status.value = article.status
    publishTime.value = article.publishTime
    scheduledPublishTime.value = article.scheduledPublishTime
    form.body = article.body
    capture()
    if (route.name === 'article-create') {
      await router.replace(`/articles/${article.contentId}`)
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
  const message = validateArticleForm(form, leafIds.value)
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

function onInsertVideoQr(payload: { contentId: string; title: string; url: string }) {
  const html = buildVideoMiniprogramQrHtml(payload.url, payload.title, payload.contentId)
  editorRef.value?.insertHtml(html)
}

watch(
  () => [route.name, route.params.id] as const,
  () => {
    const next = route.name === 'article-create' ? '' : String(route.params.id || '')
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
      <el-button @click="router.push('/articles')">返回内容列表</el-button>
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
        <el-input model-value="文章" disabled />
      </el-form-item>
      <el-form-item label="作者">
        <el-input v-model="form.author" maxlength="64" />
      </el-form-item>
      <el-form-item label="来源">
        <el-input v-model="form.source" maxlength="128" />
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
      <el-form-item label="正文">
        <div class="body-toolbar">
          <el-button :disabled="!canSave || busy" @click="qrDialogVisible = true">
            插入视频小程序码
          </el-button>
          <span class="body-hint">
            光标放在题后插入；点选后拖四角改大小，拖图片可自由移动，也可用工具栏左/中/右对齐
          </span>
        </div>
        <RichTextEditor ref="editorRef" v-model="form.body" />
      </el-form-item>
    </el-form>
    <VideoMiniprogramQrDialog v-model="qrDialogVisible" @insert="onInsertVideoQr" />
    <el-dialog v-model="previewVisible" title="文章预览" width="720px">
      <article class="preview">
        <h1>{{ form.title }}</h1>
        <p>作者：{{ form.author }}</p>
        <p>来源：{{ form.source }}</p>
        <div class="preview-body" v-html="safeBody" />
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

.preview h1 {
  margin: 0 0 12px;
  font-size: 22px;
  line-height: 1.4;
}

.preview p {
  margin: 0 0 8px;
  color: #666;
}

.preview-body :deep(img) {
  max-width: 100%;
}

.body-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  width: 100%;
}

.body-hint {
  color: #909399;
  font-size: 12px;
}
</style>

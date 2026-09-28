<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { categoryApi } from '@/api/category'
import { contentApi } from '@/api/content'
import { questionApi } from '@/api/question'
import ContentScheduleActions from '@/components/ContentScheduleActions.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { useUnsavedLeave } from '@/composables/useUnsavedLeave'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types/category'
import type { ContentStatus } from '@/types/content'
import type { QuestionDifficulty, QuestionType } from '@/types/question'
import { normalizeTree } from '@/utils/category-form'
import { categoryOptions, displayStatusLabel, formatTime, offlineConfirmText, publishActionLabel, publishConfirmText, saveSuccessMessage } from '@/utils/content-form'
import {
  difficultyLabel,
  questionTypeLabel,
  toQuestionPayload,
  validateQuestionForm,
  type QuestionFormValues,
} from '@/utils/question-form'
import { resolveMediaUrl } from '@/utils/media-url'

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const offlining = ref(false)
const loadError = ref('')
const previewVisible = ref(false)
const answerCollapsed = ref(true)
const analysisCollapsed = ref(true)
const categories = ref<Category[]>([])
const formRef = ref<FormInstance>()
const contentId = ref(route.name === 'question-create' ? '' : String(route.params.id || ''))
const status = ref<ContentStatus>('DRAFT')
const publishTime = ref<string | null>(null)
const scheduledPublishTime = ref<string | null>(null)
const snapshot = ref('')

const questionTypes: QuestionType[] = [
  'SINGLE_CHOICE',
  'MULTIPLE_CHOICE',
  'FILL_BLANK',
  'ANSWER',
  'PROOF',
  'IMAGE_QUESTION',
]
const difficulties: QuestionDifficulty[] = ['EASY', 'MEDIUM', 'HARD']

const form = reactive<QuestionFormValues>({
  title: '',
  categoryId: '',
  coverUrl: '',
  summary: '',
  sort: 0,
  questionType: '',
  questionText: '',
  questionImageUrl: '',
  answerText: '',
  answerImageUrl: '',
  analysisText: '',
  analysisImageUrl: '',
  difficulty: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  questionType: [{ required: true, message: '请选择题型', trigger: 'change' }],
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
const typeLabel = computed(() => (form.questionType ? questionTypeLabel(form.questionType) : ''))
const difficultyText = computed(() => (form.difficulty ? difficultyLabel(form.difficulty) : ''))

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
    const [categoryResponse, questionResponse] = await Promise.all([
      categoryApi.getCategoryTree(),
      questionApi.getQuestion(contentId.value),
    ])
    categories.value = normalizeTree(categoryResponse.data.data ?? [])
    const question = questionResponse.data.data
    form.title = question.title
    form.categoryId = String(question.categoryId)
    form.coverUrl = question.coverUrl ?? ''
    form.summary = question.summary ?? ''
    form.sort = question.sort
    form.questionType = question.questionType ?? ''
    form.questionText = question.questionText ?? ''
    form.questionImageUrl = question.questionImageUrl ?? ''
    form.answerText = question.answerText ?? ''
    form.answerImageUrl = question.answerImageUrl ?? ''
    form.analysisText = question.analysisText ?? ''
    form.analysisImageUrl = question.analysisImageUrl ?? ''
    form.difficulty = question.difficulty ?? ''
    status.value = question.status
    publishTime.value = question.publishTime
    scheduledPublishTime.value = question.scheduledPublishTime
    capture()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '获取题目失败'
  } finally {
    loading.value = false
  }
}

async function saveDraft() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return false
  }
  const message = validateQuestionForm(form, leafIds.value)
  if (message) {
    ElMessage.error(message)
    return false
  }
  saving.value = true
  try {
    const payload = toQuestionPayload(form)
    const response = creating.value
      ? await questionApi.createQuestion(payload)
      : await questionApi.updateQuestion(contentId.value, payload)
    const question = response.data.data
    contentId.value = String(question.contentId)
    status.value = question.status
    publishTime.value = question.publishTime
    scheduledPublishTime.value = question.scheduledPublishTime
    capture()
    if (route.name === 'question-create') {
      await router.replace(`/questions/${question.contentId}`)
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
  const message = validateQuestionForm(form, leafIds.value)
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
    const next = route.name === 'question-create' ? '' : String(route.params.id || '')
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
      <el-button @click="router.push('/questions')">返回内容列表</el-button>
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
        <el-input model-value="题目" disabled />
      </el-form-item>
      <el-form-item label="题型" prop="questionType">
        <el-select v-model="form.questionType" placeholder="请选择题型">
          <el-option v-for="item in questionTypes" :key="item" :label="questionTypeLabel(item)" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="难度">
        <el-select v-model="form.difficulty" clearable placeholder="可选">
          <el-option v-for="item in difficulties" :key="item" :label="difficultyLabel(item)" :value="item" />
        </el-select>
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
      <el-form-item label="题目内容">
        <el-input v-model="form.questionText" type="textarea" :rows="6" maxlength="20000" show-word-limit />
      </el-form-item>
      <el-form-item label="题目图片">
        <ImageUpload v-model="form.questionImageUrl" scene="QUESTION" :disabled="!canSave" />
      </el-form-item>
      <el-form-item label="答案">
        <el-input v-model="form.answerText" type="textarea" :rows="4" maxlength="20000" show-word-limit />
      </el-form-item>
      <el-form-item label="答案图片">
        <ImageUpload v-model="form.answerImageUrl" scene="QUESTION" :disabled="!canSave" />
      </el-form-item>
      <el-form-item label="解析">
        <el-input v-model="form.analysisText" type="textarea" :rows="4" maxlength="20000" show-word-limit />
      </el-form-item>
      <el-form-item label="解析图片">
        <ImageUpload v-model="form.analysisImageUrl" scene="QUESTION" :disabled="!canSave" />
      </el-form-item>
    </el-form>
    <el-dialog v-model="previewVisible" title="题目预览" width="720px" @closed="answerCollapsed = true; analysisCollapsed = true">
      <div class="preview">
        <h1>{{ form.title }}</h1>
        <p>题型：{{ typeLabel }}</p>
        <p v-if="difficultyText">难度：{{ difficultyText }}</p>
        <p v-if="form.summary">摘要：{{ form.summary }}</p>
        <pre class="text-block">{{ form.questionText }}</pre>
        <el-image
          v-if="form.questionImageUrl"
          :src="resolveMediaUrl(form.questionImageUrl, apiBase)"
          fit="contain"
          class="preview-image"
        />
        <div class="collapse-block">
          <el-button link type="primary" @click="answerCollapsed = !answerCollapsed">
            {{ answerCollapsed ? '展开答案' : '收起答案' }}
          </el-button>
          <div v-show="!answerCollapsed">
            <pre class="text-block">{{ form.answerText }}</pre>
            <el-image
              v-if="form.answerImageUrl"
              :src="resolveMediaUrl(form.answerImageUrl, apiBase)"
              fit="contain"
              class="preview-image"
            />
          </div>
        </div>
        <div class="collapse-block">
          <el-button link type="primary" @click="analysisCollapsed = !analysisCollapsed">
            {{ analysisCollapsed ? '展开解析' : '收起解析' }}
          </el-button>
          <div v-show="!analysisCollapsed">
            <pre class="text-block">{{ form.analysisText }}</pre>
            <el-image
              v-if="form.analysisImageUrl"
              :src="resolveMediaUrl(form.analysisImageUrl, apiBase)"
              fit="contain"
              class="preview-image"
            />
          </div>
        </div>
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
}

.text-block {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 8px 0;
  font-family: inherit;
}

.preview-image {
  max-width: 100%;
  max-height: 240px;
  margin: 8px 0;
}

.collapse-block {
  margin-top: 12px;
}
</style>

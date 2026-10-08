<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { categoryApi } from '@/api/category'
import { contentApi } from '@/api/content'
import { videoApi } from '@/api/video'
import ContentScheduleActions from '@/components/ContentScheduleActions.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import VideoFileUpload from '@/components/VideoFileUpload.vue'
import { useUnsavedLeave } from '@/composables/useUnsavedLeave'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types/category'
import type { ContentStatus } from '@/types/content'
import type { FileUploadResult } from '@/types/file'
import { normalizeTree } from '@/utils/category-form'
import { categoryOptions, displayStatusLabel, formatTime, offlineConfirmText, publishActionLabel, publishConfirmText, saveSuccessMessage } from '@/utils/content-form'
import { resolveMediaUrl } from '@/utils/media-url'
import {
  canPreviewQr,
  fileNameFromUrl,
  formatDuration,
  isLocalSource,
  sourceTypeLabel,
  toVideoPayload,
  validateVideoForm,
  validateVideoPublish,
  type VideoFormValues,
} from '@/utils/video-form'

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
const qrViewer = ref(false)
const categories = ref<Category[]>([])
const formRef = ref<FormInstance>()
const contentId = ref(route.name === 'video-create' ? '' : String(route.params.id || ''))
const status = ref<ContentStatus>('DRAFT')
const publishTime = ref<string | null>(null)
const scheduledPublishTime = ref<string | null>(null)
const snapshot = ref('')

const form = reactive<VideoFormValues>({
  title: '',
  categoryId: '',
  coverUrl: '',
  summary: '',
  sort: 0,
  sourceType: 'LOCAL',
  videoUrl: '',
  qrCodeUrl: '',
  duration: null,
})
const videoFileName = ref('')

const rules: FormRules = {
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  sourceType: [{ required: true, message: '请选择视频来源', trigger: 'change' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'change' }],
}

const creating = computed(() => contentId.value === '')
const options = computed(() => categoryOptions(categories.value))
const leafIds = computed(() => options.value.filter((item) => item.leaf).map((item) => item.id))
const formOptions = computed(() => options.value.filter((item) => item.leaf || item.id === form.categoryId))
const canSave = computed(() => authStore.permissions.includes('VIDEO_MANAGE'))
const canPublish = computed(() => status.value !== 'PUBLISHED' && authStore.permissions.includes('CONTENT_PUBLISH'))
const canOffline = computed(() => status.value === 'PUBLISHED' && authStore.permissions.includes('CONTENT_OFFLINE'))
const busy = computed(() => saving.value || publishing.value || offlining.value)
const dirty = computed(() => snapshot.value !== '' && snapshot.value !== JSON.stringify(form))
const qrPreview = computed(() => (canPreviewQr(form.qrCodeUrl) ? resolveMediaUrl(form.qrCodeUrl, apiBase) : ''))
const durationText = computed(() => formatDuration(form.duration))
const sourceLabel = computed(() => (form.sourceType ? sourceTypeLabel(form.sourceType) : ''))
const localSource = computed(() => isLocalSource(form.sourceType))
const localPlayUrl = computed(() => (localSource.value && form.videoUrl ? resolveMediaUrl(form.videoUrl, apiBase) : ''))

useUnsavedLeave(dirty)

function applyPublishState(value: { status: ContentStatus; scheduledPublishTime: string | null; publishTime: string | null }) {
  status.value = value.status
  scheduledPublishTime.value = value.scheduledPublishTime
  publishTime.value = value.publishTime
}

function capture() {
  snapshot.value = JSON.stringify(form)
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
    const [categoryResponse, videoResponse] = await Promise.all([
      categoryApi.getCategoryTree(),
      videoApi.getVideo(contentId.value),
    ])
    categories.value = normalizeTree(categoryResponse.data.data ?? [])
    const video = videoResponse.data.data
    form.title = video.title
    form.categoryId = video.categoryId
    form.coverUrl = video.coverUrl ?? ''
    form.summary = video.summary ?? ''
    form.sort = video.sort
    form.videoUrl = video.videoUrl ?? ''
    form.qrCodeUrl = video.qrCodeUrl ?? ''
    form.duration = video.duration
    form.sourceType = video.sourceType ?? 'LOCAL'
    videoFileName.value = fileNameFromUrl(form.videoUrl)
    status.value = video.status
    publishTime.value = video.publishTime
    scheduledPublishTime.value = video.scheduledPublishTime
    capture()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '获取视频失败'
  } finally {
    loading.value = false
  }
}

async function saveDraft() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return false
  }
  const message = validateVideoForm(form, leafIds.value)
  if (message) {
    ElMessage.error(message)
    return false
  }
  saving.value = true
  try {
    const payload = toVideoPayload(form)
    const response = creating.value
      ? await videoApi.createVideo(payload)
      : await videoApi.updateVideo(contentId.value, payload)
    const video = response.data.data
    contentId.value = video.contentId
    status.value = video.status
    publishTime.value = video.publishTime
    scheduledPublishTime.value = video.scheduledPublishTime
    capture()
    if (route.name === 'video-create') {
      await router.replace(`/videos/${video.contentId}`)
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
  const message = validateVideoPublish(form, leafIds.value)
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

function onLocalVideoUploaded(result: FileUploadResult, duration: number | null) {
  form.videoUrl = result.url
  videoFileName.value = result.fileName || fileNameFromUrl(result.url)
  if (duration != null && (form.duration == null || form.duration === 0)) {
    form.duration = duration
  }
}

function clearLocalVideo() {
  form.videoUrl = ''
  videoFileName.value = ''
}

function openQr() {
  if (qrPreview.value) {
    qrViewer.value = true
  }
}

watch(
  () => form.sourceType,
  (next, prev) => {
    if (!prev || next === prev) {
      return
    }
    if (isLocalSource(next)) {
      form.qrCodeUrl = ''
      if (form.videoUrl && !form.videoUrl.startsWith('/')) {
        form.videoUrl = ''
        videoFileName.value = ''
      }
      return
    }
    if (form.videoUrl.startsWith('/')) {
      form.videoUrl = ''
      videoFileName.value = ''
    }
  },
)

watch(
  () => [route.name, route.params.id] as const,
  () => {
    const next = route.name === 'video-create' ? '' : String(route.params.id || '')
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
      <el-button @click="router.push('/videos')">返回内容列表</el-button>
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
        <el-input model-value="视频" disabled />
      </el-form-item>
      <el-form-item label="视频来源" prop="sourceType">
        <el-select v-model="form.sourceType" placeholder="请选择视频来源">
          <el-option label="本地上传" value="LOCAL" />
          <el-option label="微信视频号" value="WECHAT_CHANNEL" />
          <el-option label="腾讯视频" value="TENCENT_VIDEO" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="localSource" label="视频文件">
        <VideoFileUpload
          :file-url="form.videoUrl"
          :file-name="videoFileName"
          :disabled="!canSave"
          @success="onLocalVideoUploaded"
          @clear="clearLocalVideo"
        />
      </el-form-item>
      <el-form-item v-else label="视频地址">
        <el-input v-model="form.videoUrl" maxlength="512" placeholder="https://" />
      </el-form-item>
      <el-form-item label="封面">
        <ImageUpload v-model="form.coverUrl" scene="VIDEO" :disabled="!canSave" />
      </el-form-item>
      <el-form-item v-if="!localSource" label="二维码">
        <ImageUpload v-model="form.qrCodeUrl" scene="QRCODE" :disabled="!canSave" />
        <div v-if="qrPreview" class="qr-box">
          <el-button link type="primary" @click="openQr">查看二维码</el-button>
        </div>
      </el-form-item>
      <el-form-item label="时长（秒）">
        <el-input-number v-model="form.duration" :min="0" :step="1" />
        <span v-if="durationText" class="duration">{{ durationText }}</span>
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.summary" type="textarea" maxlength="512" show-word-limit />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="form.sort" :min="0" :max="9999" />
      </el-form-item>
    </el-form>
    <el-dialog v-model="previewVisible" title="视频预览" width="640px">
      <div class="preview">
        <h1>{{ form.title }}</h1>
        <p>视频来源：{{ sourceLabel }}</p>
        <video v-if="localPlayUrl" class="player" :src="localPlayUrl" controls preload="metadata" />
        <p v-else>视频地址：{{ form.videoUrl }}</p>
        <p>时长：{{ form.duration ?? '' }}<span v-if="durationText">（{{ durationText }}）</span></p>
        <p>简介：{{ form.summary }}</p>
        <div v-if="form.coverUrl" class="media">
          <span>封面</span>
          <el-image :src="resolveMediaUrl(form.coverUrl, apiBase)" fit="contain" class="cover-image">
            <template #error>封面加载失败</template>
          </el-image>
        </div>
        <div v-if="!localSource" class="media">
          <span>二维码</span>
          <el-image v-if="qrPreview" :src="qrPreview" :preview-src-list="[qrPreview]" fit="contain" class="qr-image">
            <template #error>二维码加载失败</template>
          </el-image>
          <p v-else>二维码加载失败</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <el-image-viewer v-if="qrViewer" :url-list="[qrPreview]" @close="qrViewer = false" />
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

.qr-box,
.media {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.qr-image,
.cover-image {
  width: 120px;
  height: 120px;
}

.duration {
  margin-left: 12px;
  color: #666;
}

.player {
  width: 100%;
  max-height: 360px;
  background: #000;
}

.preview h1 {
  margin: 0 0 12px;
  font-size: 22px;
}
</style>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { articleApi } from '@/api/article'
import { documentApi } from '@/api/document'
import { questionApi } from '@/api/question'
import { videoApi } from '@/api/video'
import { weeklyApi } from '@/api/weekly'
import type { ArticleDetail } from '@/types/article'
import type { ContentType } from '@/types/content'
import type { DocumentDetail } from '@/types/document'
import type { QuestionDetail } from '@/types/question'
import type { VideoDetail } from '@/types/video'
import type { WeeklyDetail } from '@/types/weekly'
import { previewHtml } from '@/utils/article-form'
import { contentTypeLabel, formatTime, statusLabel } from '@/utils/content-form'
import { formatFileSize } from '@/utils/file-upload'
import { resolveMediaUrl } from '@/utils/media-url'
import { difficultyLabel, questionTypeLabel } from '@/utils/question-form'
import { formatDuration, sourceTypeLabel } from '@/utils/video-form'

const props = defineProps<{
  visible: boolean
  contentId: string
  contentType: ContentType
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const loading = ref(false)
const error = ref('')
const article = ref<ArticleDetail | null>(null)
const video = ref<VideoDetail | null>(null)
const question = ref<QuestionDetail | null>(null)
const weekly = ref<WeeklyDetail | null>(null)
const documentDetail = ref<DocumentDetail | null>(null)
const answerCollapsed = ref(true)
const analysisCollapsed = ref(true)

const open = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})

const title = computed(() => `${contentTypeLabel(props.contentType)}预览`)
const safeBody = computed(() => previewHtml(article.value?.body ?? ''))
const coverUrl = computed(() => {
  const raw =
    props.contentType === 'ARTICLE'
      ? article.value?.coverUrl
      : props.contentType === 'VIDEO'
        ? video.value?.coverUrl
        : props.contentType === 'QUESTION'
          ? question.value?.coverUrl
          : props.contentType === 'WEEKLY'
            ? weekly.value?.coverUrl
            : documentDetail.value?.coverUrl
  return raw ? resolveMediaUrl(raw, apiBase) : ''
})
const qrUrl = computed(() => {
  const raw = video.value?.qrCodeUrl
  return raw ? resolveMediaUrl(raw, apiBase) : ''
})
const documentSize = computed(() => formatFileSize(documentDetail.value?.fileSize))

function media(url: string | null | undefined) {
  return url ? resolveMediaUrl(url, apiBase) : ''
}

async function loadPreview() {
  if (!props.contentId) {
    return
  }
  loading.value = true
  error.value = ''
  article.value = null
  video.value = null
  question.value = null
  weekly.value = null
  documentDetail.value = null
  answerCollapsed.value = true
  analysisCollapsed.value = true
  try {
    if (props.contentType === 'ARTICLE') {
      const response = await articleApi.getArticle(props.contentId)
      article.value = response.data.data
    } else if (props.contentType === 'VIDEO') {
      const response = await videoApi.getVideo(props.contentId)
      video.value = response.data.data
    } else if (props.contentType === 'QUESTION') {
      const response = await questionApi.getQuestion(props.contentId)
      question.value = response.data.data
    } else if (props.contentType === 'WEEKLY') {
      const response = await weeklyApi.getWeekly(props.contentId)
      weekly.value = response.data.data
    } else {
      const response = await documentApi.getDocument(props.contentId)
      documentDetail.value = response.data.data
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '预览加载失败'
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.visible, props.contentId, props.contentType] as const,
  ([visible]) => {
    if (visible) {
      void loadPreview()
    }
  },
)
</script>

<template>
  <el-dialog v-model="open" :title="title" width="720px" destroy-on-close>
    <div v-loading="loading">
      <el-alert v-if="error" type="error" :closable="false" show-icon :title="error" class="preview-alert">
        <el-button link type="primary" @click="loadPreview">重新加载</el-button>
      </el-alert>

      <article v-else-if="article" class="preview">
        <h1>{{ article.title }}</h1>
        <p class="meta">
          {{ contentTypeLabel('ARTICLE') }} · {{ statusLabel(article.status) }}
          <span v-if="article.author"> · {{ article.author }}</span>
          <span v-if="article.source"> · {{ article.source }}</span>
        </p>
        <p v-if="article.summary" class="summary">{{ article.summary }}</p>
        <el-image v-if="coverUrl" :src="coverUrl" fit="contain" class="cover" />
        <div class="body" v-html="safeBody" />
      </article>

      <div v-else-if="video" class="preview">
        <h1>{{ video.title }}</h1>
        <p class="meta">
          {{ contentTypeLabel('VIDEO') }} · {{ statusLabel(video.status) }}
          <span v-if="video.sourceType"> · {{ sourceTypeLabel(video.sourceType) }}</span>
          <span v-if="video.duration != null"> · {{ formatDuration(video.duration) }}</span>
        </p>
        <p v-if="video.summary" class="summary">{{ video.summary }}</p>
        <el-image v-if="coverUrl" :src="coverUrl" fit="contain" class="cover" />
        <p v-if="video.videoUrl" class="link">视频地址：{{ video.videoUrl }}</p>
        <div v-if="qrUrl" class="qr">
          <p>二维码</p>
          <el-image :src="qrUrl" fit="contain" class="qr-image" />
        </div>
        <p v-if="video.publishTime" class="meta">发布时间：{{ formatTime(video.publishTime) }}</p>
      </div>

      <div v-else-if="question" class="preview">
        <h1>{{ question.title }}</h1>
        <p class="meta">
          {{ contentTypeLabel('QUESTION') }} · {{ statusLabel(question.status) }}
          <span v-if="question.questionType"> · {{ questionTypeLabel(question.questionType) }}</span>
          <span v-if="question.difficulty"> · {{ difficultyLabel(question.difficulty) }}</span>
        </p>
        <p v-if="question.summary" class="summary">{{ question.summary }}</p>
        <el-image v-if="coverUrl" :src="coverUrl" fit="contain" class="cover" />
        <pre class="text-block">{{ question.questionText }}</pre>
        <el-image v-if="question.questionImageUrl" :src="media(question.questionImageUrl)" fit="contain" class="media-image" />
        <div class="collapse-block">
          <el-button link type="primary" @click="answerCollapsed = !answerCollapsed">
            {{ answerCollapsed ? '展开答案' : '收起答案' }}
          </el-button>
          <div v-show="!answerCollapsed">
            <pre class="text-block">{{ question.answerText }}</pre>
            <el-image v-if="question.answerImageUrl" :src="media(question.answerImageUrl)" fit="contain" class="media-image" />
          </div>
        </div>
        <div class="collapse-block">
          <el-button link type="primary" @click="analysisCollapsed = !analysisCollapsed">
            {{ analysisCollapsed ? '展开解析' : '收起解析' }}
          </el-button>
          <div v-show="!analysisCollapsed">
            <pre class="text-block">{{ question.analysisText }}</pre>
            <el-image v-if="question.analysisImageUrl" :src="media(question.analysisImageUrl)" fit="contain" class="media-image" />
          </div>
        </div>
      </div>

      <div v-else-if="weekly" class="preview">
        <h1>{{ weekly.title }}</h1>
        <p class="meta">
          {{ contentTypeLabel('WEEKLY') }} · {{ statusLabel(weekly.status) }}
          <span v-if="weekly.weekLabel"> · {{ weekly.weekLabel }}</span>
        </p>
        <p v-if="weekly.summary" class="summary">{{ weekly.summary }}</p>
        <el-image v-if="coverUrl" :src="coverUrl" fit="contain" class="cover" />
        <pre class="text-block">{{ weekly.questionText }}</pre>
        <el-image v-if="weekly.questionImageUrl" :src="media(weekly.questionImageUrl)" fit="contain" class="media-image" />
        <div class="collapse-block">
          <el-button link type="primary" @click="answerCollapsed = !answerCollapsed">
            {{ answerCollapsed ? '展开答案' : '收起答案' }}
          </el-button>
          <div v-show="!answerCollapsed">
            <pre class="text-block">{{ weekly.answerText }}</pre>
            <el-image v-if="weekly.answerImageUrl" :src="media(weekly.answerImageUrl)" fit="contain" class="media-image" />
          </div>
        </div>
        <div class="collapse-block">
          <el-button link type="primary" @click="analysisCollapsed = !analysisCollapsed">
            {{ analysisCollapsed ? '展开解析' : '收起解析' }}
          </el-button>
          <div v-show="!analysisCollapsed">
            <pre class="text-block">{{ weekly.analysisText }}</pre>
            <el-image v-if="weekly.analysisImageUrl" :src="media(weekly.analysisImageUrl)" fit="contain" class="media-image" />
          </div>
        </div>
      </div>

      <div v-else-if="documentDetail" class="preview">
        <h1>{{ documentDetail.title }}</h1>
        <p class="meta">{{ contentTypeLabel('DOCUMENT') }} · {{ statusLabel(documentDetail.status) }}</p>
        <p v-if="documentDetail.summary" class="summary">{{ documentDetail.summary }}</p>
        <p v-if="documentDetail.description" class="summary">{{ documentDetail.description }}</p>
        <el-image v-if="coverUrl" :src="coverUrl" fit="contain" class="cover" />
        <p class="link">文件名：{{ documentDetail.fileName || '—' }}</p>
        <p class="link">类型：{{ documentDetail.fileType || '—' }}</p>
        <p class="link">大小：{{ documentSize || '—' }}</p>
        <p v-if="documentDetail.fileUrl" class="link">文件地址：{{ documentDetail.fileUrl }}</p>
        <p v-if="documentDetail.downloadUrl" class="link">下载地址：{{ documentDetail.downloadUrl }}</p>
        <p v-if="documentDetail.previewUrl" class="link">预览地址：{{ documentDetail.previewUrl }}</p>
      </div>

      <el-empty v-else-if="!loading && !error" description="暂无预览数据" :image-size="72" />
    </div>
    <template #footer>
      <el-button @click="open = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.preview-alert {
  margin-bottom: 12px;
}

.preview h1 {
  margin: 0 0 8px;
  font-size: 22px;
}

.meta,
.summary,
.link {
  color: #646a73;
  line-height: 1.6;
  word-break: break-all;
}

.cover {
  display: block;
  width: 100%;
  max-height: 280px;
  margin: 12px 0;
}

.body :deep(img) {
  max-width: 100%;
}

.qr-image,
.media-image {
  width: 160px;
  height: 160px;
  max-width: 100%;
}

.text-block {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 8px 0;
  font-family: inherit;
}

.collapse-block {
  margin-top: 12px;
}
</style>

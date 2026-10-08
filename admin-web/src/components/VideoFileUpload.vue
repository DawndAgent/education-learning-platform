<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { fileApi } from '@/api/file'
import type { FileUploadResult } from '@/types/file'
import { validateVideoFile } from '@/utils/file-upload'
import { resolveMediaUrl } from '@/utils/media-url'

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const props = withDefaults(
  defineProps<{
    fileUrl?: string
    fileName?: string
    disabled?: boolean
  }>(),
  {
    fileUrl: '',
    fileName: '',
    disabled: false,
  },
)

const emit = defineEmits<{
  success: [result: FileUploadResult, duration: number | null]
  clear: []
  error: [message: string]
}>()

const uploading = ref(false)
const percent = ref(0)
const inputRef = ref<HTMLInputElement>()

const hasFile = computed(() => Boolean(props.fileUrl))
const previewUrl = computed(() => (props.fileUrl ? resolveMediaUrl(props.fileUrl, apiBase) : ''))
const displayName = computed(() => props.fileName || '已上传视频')

function openPicker() {
  if (props.disabled || uploading.value) {
    return
  }
  inputRef.value?.click()
}

async function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  const message = validateVideoFile(file)
  if (message) {
    ElMessage.error(message)
    emit('error', message)
    return
  }
  uploading.value = true
  percent.value = 0
  try {
    const duration = await readVideoDuration(file)
    const response = await fileApi.uploadVideo(file, {
      onProgress(value) {
        percent.value = value
      },
    })
    const result = response.data.data
    percent.value = 100
    emit('success', result, duration)
    ElMessage.success('上传成功')
  } catch (error) {
    const text = error instanceof Error ? error.message : '上传失败'
    emit('error', text)
  } finally {
    uploading.value = false
  }
}

function clear() {
  if (props.disabled || uploading.value) {
    return
  }
  emit('clear')
}

function readVideoDuration(file: File): Promise<number | null> {
  return new Promise((resolve) => {
    const el = document.createElement('video')
    el.preload = 'metadata'
    el.onloadedmetadata = () => {
      const seconds = Math.round(el.duration)
      URL.revokeObjectURL(el.src)
      resolve(Number.isFinite(seconds) && seconds >= 0 ? seconds : null)
    }
    el.onerror = () => {
      URL.revokeObjectURL(el.src)
      resolve(null)
    }
    el.src = URL.createObjectURL(file)
  })
}
</script>

<template>
  <div class="video-upload">
    <input ref="inputRef" class="hidden-input" type="file" accept="video/mp4,.mp4" @change="onFileChange" />
    <div v-if="hasFile" class="file-info">
      <p class="file-name">{{ displayName }}</p>
      <video v-if="previewUrl" class="player" :src="previewUrl" controls preload="metadata" />
      <div class="actions">
        <el-button link type="primary" :disabled="disabled || uploading" @click="openPicker">重新上传</el-button>
        <el-button link type="danger" :disabled="disabled || uploading" :icon="Delete" @click="clear">删除</el-button>
      </div>
    </div>
    <el-button v-else :icon="Plus" :loading="uploading" :disabled="disabled" @click="openPicker">
      上传 MP4 视频
    </el-button>
    <p class="hint">支持 MP4，最大 50MB。发布后可在小程序内直接播放。</p>
    <el-progress v-if="uploading" :percentage="percent" :stroke-width="4" />
  </div>
</template>

<style scoped>
.video-upload {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.hidden-input {
  display: none;
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-name {
  margin: 0;
  font-weight: 500;
}

.player {
  width: 360px;
  max-width: 100%;
  background: #000;
}

.actions {
  display: flex;
  gap: 8px;
}

.hint {
  margin: 0;
  color: #646a73;
  font-size: 13px;
}
</style>

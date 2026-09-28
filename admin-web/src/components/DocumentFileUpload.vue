<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { fileApi } from '@/api/file'
import type { FileUploadResult } from '@/types/file'
import { formatFileSize, validateDocumentFile } from '@/utils/file-upload'

const props = withDefaults(
  defineProps<{
    fileUrl?: string
    fileName?: string
    fileSize?: number | null
    fileType?: string
    disabled?: boolean
  }>(),
  {
    fileUrl: '',
    fileName: '',
    fileSize: null,
    fileType: '',
    disabled: false,
  },
)

const emit = defineEmits<{
  success: [result: FileUploadResult]
  clear: []
  error: [message: string]
}>()

const uploading = ref(false)
const percent = ref(0)
const inputRef = ref<HTMLInputElement>()

const hasFile = computed(() => Boolean(props.fileUrl || props.fileName))
const sizeText = computed(() => formatFileSize(props.fileSize))

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
  const message = validateDocumentFile(file)
  if (message) {
    ElMessage.error(message)
    emit('error', message)
    return
  }
  uploading.value = true
  percent.value = 0
  try {
    const response = await fileApi.uploadDocument(file, {
      onProgress(value) {
        percent.value = value
      },
    })
    const result = response.data.data
    percent.value = 100
    emit('success', result)
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
</script>

<template>
  <div class="document-upload">
    <input
      ref="inputRef"
      class="hidden-input"
      type="file"
      accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument.presentationml.presentation"
      @change="onFileChange"
    />
    <div v-if="hasFile" class="file-info">
      <p class="file-name">{{ fileName || '已上传文件' }}</p>
      <p v-if="fileType || sizeText" class="file-meta">
        <span v-if="fileType">{{ fileType }}</span>
        <span v-if="fileType && sizeText"> · </span>
        <span v-if="sizeText">{{ sizeText }}</span>
      </p>
      <div class="actions">
        <el-button link type="primary" :disabled="disabled || uploading" @click="openPicker">重新上传</el-button>
        <el-button link type="danger" :disabled="disabled || uploading" :icon="Delete" @click="clear">删除</el-button>
      </div>
    </div>
    <el-button v-else :icon="Plus" :loading="uploading" :disabled="disabled" @click="openPicker">
      上传资料
    </el-button>
    <el-progress v-if="uploading" :percentage="percent" :stroke-width="4" />
  </div>
</template>

<style scoped>
.document-upload {
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
  gap: 4px;
}

.file-name {
  margin: 0;
  font-weight: 500;
}

.file-meta {
  margin: 0;
  color: #646a73;
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 8px;
}
</style>

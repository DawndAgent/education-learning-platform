<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { fileApi } from '@/api/file'
import type { UploadScene } from '@/types/file'
import { validateImageFile } from '@/utils/file-upload'
import { resolveMediaUrl } from '@/utils/media-url'

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const model = defineModel<string>({ default: '' })

const props = withDefaults(
  defineProps<{
    scene?: UploadScene
    accept?: string
    maxSize?: number
    disabled?: boolean
  }>(),
  {
    scene: 'COVER',
    accept: 'image/jpeg,image/png,image/webp,image/gif',
    maxSize: 10 * 1024 * 1024,
    disabled: false,
  },
)

const emit = defineEmits<{
  success: [url: string]
  error: [message: string]
}>()

const uploading = ref(false)
const percent = ref(0)
const inputRef = ref<HTMLInputElement>()

const preview = computed(() => (model.value ? resolveMediaUrl(model.value, apiBase) : ''))

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
  const message = validateImageFile(file, props.maxSize)
  if (message) {
    ElMessage.error(message)
    emit('error', message)
    return
  }
  uploading.value = true
  percent.value = 0
  try {
    const response = await fileApi.uploadImage(file, {
      scene: props.scene,
      onProgress(value) {
        percent.value = value
      },
    })
    const url = response.data.data.url
    model.value = url
    percent.value = 100
    emit('success', url)
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
  model.value = ''
}
</script>

<template>
  <div class="image-upload">
    <input ref="inputRef" class="hidden-input" type="file" :accept="accept" @change="onFileChange" />
    <div v-if="preview" class="preview">
      <el-image :src="preview" fit="cover" class="preview-image">
        <template #error>图片加载失败</template>
      </el-image>
      <div class="actions">
        <el-button link type="primary" :disabled="disabled || uploading" @click="openPicker">重新上传</el-button>
        <el-button link type="danger" :disabled="disabled || uploading" :icon="Delete" @click="clear">删除</el-button>
      </div>
    </div>
    <el-button v-else :icon="Plus" :loading="uploading" :disabled="disabled" @click="openPicker">
      上传图片
    </el-button>
    <el-progress v-if="uploading" :percentage="percent" :stroke-width="4" />
  </div>
</template>

<style scoped>
.image-upload {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.hidden-input {
  display: none;
}

.preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-image {
  width: 160px;
  height: 160px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.actions {
  display: flex;
  gap: 8px;
}
</style>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { contentApi } from '@/api/content'
import type { ContentStatus } from '@/types/content'

const props = defineProps<{
  contentId: string
  status: ContentStatus
  scheduledPublishTime: string | null
  title: string
  disabled: boolean
  canPublish: boolean
}>()

const emit = defineEmits<{
  updated: [value: { status: ContentStatus; scheduledPublishTime: string | null; publishTime: string | null }]
}>()

const dialogVisible = ref(false)
const publishAt = ref('')
const submitting = ref(false)

const showSchedule = computed(
  () => props.canPublish && props.contentId !== '' && !props.scheduledPublishTime && props.status !== 'PUBLISHED',
)
const showCancel = computed(
  () => props.canPublish && props.status === 'DRAFT' && Boolean(props.scheduledPublishTime),
)

function disablePastDate(date: Date): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

function openDialog() {
  publishAt.value = ''
  dialogVisible.value = true
}

async function confirmSchedule() {
  if (!publishAt.value) {
    ElMessage.error('请选择发布时间')
    return
  }
  const selected = new Date(publishAt.value.replace(' ', 'T'))
  if (Number.isNaN(selected.getTime()) || selected.getTime() <= Date.now()) {
    ElMessage.error('发布时间必须晚于当前时间')
    return
  }
  try {
    await ElMessageBox.confirm(`确认于 ${publishAt.value} 定时发布「${props.title || '该内容'}」？`, '定时发布', {
      type: 'warning',
      confirmButtonText: '确定定时发布',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  submitting.value = true
  try {
    const response = await contentApi.schedulePublish(props.contentId, publishAt.value)
    const data = response.data.data
    emit('updated', {
      status: data.status,
      scheduledPublishTime: data.scheduledPublishTime,
      publishTime: data.publishTime,
    })
    dialogVisible.value = false
    ElMessage.success('定时发布成功')
  } catch {
    // 请求拦截器已展示后端错误
  } finally {
    submitting.value = false
  }
}

async function cancelSchedule() {
  try {
    await ElMessageBox.confirm('确定取消定时发布吗？内容将保持草稿。', '取消定时发布', {
      type: 'warning',
      confirmButtonText: '取消定时发布',
      cancelButtonText: '返回',
    })
  } catch {
    return
  }
  submitting.value = true
  try {
    const response = await contentApi.cancelScheduledPublish(props.contentId)
    const data = response.data.data
    emit('updated', {
      status: data.status,
      scheduledPublishTime: data.scheduledPublishTime,
      publishTime: data.publishTime,
    })
    ElMessage.success('已取消定时发布')
  } catch {
    // 请求拦截器已展示后端错误
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-button v-if="showCancel" :disabled="disabled || submitting" @click="cancelSchedule">取消定时发布</el-button>
  <el-button v-if="showSchedule" :disabled="disabled || submitting" @click="openDialog">定时发布</el-button>
  <el-dialog v-model="dialogVisible" title="定时发布" width="420px">
    <p>请选择未来的发布时间。</p>
    <el-date-picker
      v-model="publishAt"
      type="datetime"
      value-format="YYYY-MM-DD HH:mm:ss"
      placeholder="选择发布时间"
      :disabled-date="disablePastDate"
      style="width: 100%"
    />
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="confirmSchedule">确定定时发布</el-button>
    </template>
  </el-dialog>
</template>

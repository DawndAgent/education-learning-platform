<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/category'
import { questionApi } from '@/api/question'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types/category'
import type { QuestionDifficulty, QuestionPayload, QuestionType } from '@/types/question'
import type { QuestionImportDraft, QuestionPdfParsedItem } from '@/types/question-import'
import { normalizeTree } from '@/utils/category-form'
import { categoryOptions } from '@/utils/content-form'
import { validatePdfFile } from '@/utils/file-upload'
import { difficultyLabel, questionTypeLabel } from '@/utils/question-form'

const router = useRouter()
const authStore = useAuthStore()

const questionTypes: QuestionType[] = [
  'SINGLE_CHOICE',
  'MULTIPLE_CHOICE',
  'FILL_BLANK',
  'ANSWER',
  'PROOF',
  'IMAGE_QUESTION',
]
const difficulties: QuestionDifficulty[] = ['EASY', 'MEDIUM', 'HARD']

const step = ref(0)
const categories = ref<Category[]>([])
const categoryId = ref('')
const defaultDifficulty = ref<QuestionDifficulty | ''>('')
const parsing = ref(false)
const importing = ref(false)
const fileName = ref('')
const pageCount = ref(0)
const pdfUrl = ref('')
const drafts = ref<QuestionImportDraft[]>([])
const fileInputRef = ref<HTMLInputElement>()

const options = computed(() => categoryOptions(categories.value))
const leafIds = computed(() => options.value.filter((item) => item.leaf).map((item) => item.id))
const formOptions = computed(() => options.value.filter((item) => item.leaf || item.id === categoryId.value))
const canImport = computed(() => authStore.permissions.includes('CONTENT_CREATE'))
const selectedCount = computed(() => drafts.value.filter((item) => item.selected).length)
const suspiciousCount = computed(() => drafts.value.filter((item) => item.selected && item.suspicious).length)

onMounted(async () => {
  try {
    const response = await categoryApi.getCategoryTree()
    categories.value = normalizeTree(response.data.data || [])
  } catch {
    categories.value = []
  }
})

onBeforeUnmount(() => {
  revokePdfUrl()
})

function revokePdfUrl() {
  if (pdfUrl.value) {
    URL.revokeObjectURL(pdfUrl.value)
    pdfUrl.value = ''
  }
}

function openPicker() {
  if (parsing.value || importing.value) {
    return
  }
  fileInputRef.value?.click()
}

async function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  const message = validatePdfFile(file)
  if (message) {
    ElMessage.error(message)
    return
  }
  if (!categoryId.value || !leafIds.value.includes(categoryId.value)) {
    ElMessage.error('请先选择二级分类')
    return
  }
  parsing.value = true
  try {
    const response = await questionApi.parseQuestionPdf(file)
    const data = response.data.data
    revokePdfUrl()
    pdfUrl.value = URL.createObjectURL(file)
    fileName.value = data.fileName
    pageCount.value = data.pageCount
    drafts.value = data.questions.map((item) => toDraft(item))
    step.value = 1
    ElMessage.success(`识别到 ${data.questionCount} 道题，请确认后导入`)
  } catch {
    // 请求拦截器已提示
  } finally {
    parsing.value = false
  }
}

function toDraft(item: QuestionPdfParsedItem): QuestionImportDraft {
  return {
    key: `${item.index}-${item.label}`,
    selected: true,
    title: item.title || `第${item.label}题`,
    questionType: item.questionType || 'ANSWER',
    questionText: item.questionText || '',
    answerText: item.answerText || '',
    analysisText: item.analysisText || '',
    difficulty: defaultDifficulty.value,
    suspicious: Boolean(item.suspicious),
    suspiciousReason: item.suspiciousReason,
    label: item.label,
  }
}

function selectAll(value: boolean) {
  drafts.value.forEach((item) => {
    item.selected = value
  })
}

function backToUpload() {
  step.value = 0
}

async function onConfirmImport() {
  if (!canImport.value || importing.value) {
    return
  }
  if (!categoryId.value || !leafIds.value.includes(categoryId.value)) {
    ElMessage.error('请选择二级分类')
    return
  }
  const selected = drafts.value.filter((item) => item.selected)
  if (selected.length === 0) {
    ElMessage.error('请至少保留一道题目')
    return
  }
  for (const item of selected) {
    const title = item.title.trim()
    if (!title) {
      ElMessage.error(`第${item.label}题标题不能为空`)
      return
    }
    if (title.length > 128) {
      ElMessage.error(`第${item.label}题标题不能超过128字`)
      return
    }
    if (!item.questionType) {
      ElMessage.error(`第${item.label}题请选择题型`)
      return
    }
  }
  const tip = suspiciousCount.value > 0
    ? `将导入 ${selected.length} 道题为草稿，其中 ${suspiciousCount.value} 道标记为可疑，是否继续？`
    : `将导入 ${selected.length} 道题为草稿，是否继续？`
  try {
    await ElMessageBox.confirm(tip, '确认导入', {
      type: 'warning',
      confirmButtonText: '导入',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  const items: QuestionPayload[] = selected.map((item, index) => ({
    categoryId: categoryId.value,
    title: item.title.trim(),
    coverUrl: '',
    summary: '',
    sort: index,
    questionType: item.questionType,
    questionText: item.questionText,
    questionImageUrl: '',
    answerText: item.answerText,
    answerImageUrl: '',
    analysisText: item.analysisText,
    analysisImageUrl: '',
    difficulty: item.difficulty || null,
  }))
  importing.value = true
  try {
    const response = await questionApi.batchCreateQuestions(items)
    const count = response.data.data.createdCount
    ElMessage.success(`已导入 ${count} 道题目草稿`)
    await router.push('/questions')
  } catch {
    // 请求拦截器已提示
  } finally {
    importing.value = false
  }
}
</script>

<template>
  <section class="import-page">
    <div class="toolbar">
      <el-button @click="router.push('/questions')">返回题目列表</el-button>
      <div class="toolbar-title">从 PDF 导入题目</div>
    </div>

    <el-steps :active="step" finish-status="success" align-center class="steps">
      <el-step title="上传 PDF" description="选择分类并上传试卷" />
      <el-step title="确认题目" description="对照 PDF 勾选与修改" />
      <el-step title="导入完成" description="写入题库草稿" />
    </el-steps>

    <div v-show="step === 0" class="upload-panel">
      <el-form label-width="96px" class="upload-form">
        <el-form-item label="分类" required>
          <el-select v-model="categoryId" filterable clearable placeholder="请选择二级分类" style="width: 320px">
            <el-option
              v-for="item in formOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
              :disabled="!item.leaf"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="默认难度">
          <el-select v-model="defaultDifficulty" clearable placeholder="可选" style="width: 180px">
            <el-option v-for="item in difficulties" :key="item" :label="difficultyLabel(item)" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="PDF 文件" required>
          <div class="upload-box">
            <input ref="fileInputRef" type="file" accept=".pdf,application/pdf" class="hidden-input" @change="onFileChange" />
            <el-button type="primary" :loading="parsing" :disabled="!canImport" @click="openPicker">
              选择并识别 PDF
            </el-button>
            <p class="hint">仅支持可选中文字的电子版 PDF，最大 20MB。扫描件暂不支持 OCR。</p>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <div v-show="step === 1" class="review-panel">
      <div class="review-toolbar">
        <div class="meta">
          <span>{{ fileName }}</span>
          <span>共 {{ pageCount }} 页</span>
          <span>识别 {{ drafts.length }} 题</span>
          <span>已选 {{ selectedCount }} 题</span>
          <el-tag v-if="suspiciousCount > 0" type="warning" size="small">可疑 {{ suspiciousCount }}</el-tag>
        </div>
        <div class="actions">
          <el-button :disabled="importing" @click="selectAll(true)">全选</el-button>
          <el-button :disabled="importing" @click="selectAll(false)">全不选</el-button>
          <el-button :disabled="importing" @click="backToUpload">重新上传</el-button>
          <el-button
            type="primary"
            :loading="importing"
            :disabled="!canImport || selectedCount === 0"
            @click="onConfirmImport"
          >
            确认导入 {{ selectedCount }} 题
          </el-button>
        </div>
      </div>

      <div class="review-body">
        <div class="pdf-pane">
          <iframe v-if="pdfUrl" :src="pdfUrl" title="PDF 预览" class="pdf-frame" />
          <el-empty v-else description="暂无 PDF 预览" />
        </div>
        <div class="question-pane">
          <article
            v-for="item in drafts"
            :key="item.key"
            class="question-card"
            :class="{ suspicious: item.suspicious, discarded: !item.selected }"
          >
            <header class="card-head">
              <el-checkbox v-model="item.selected">保留第 {{ item.label }} 题</el-checkbox>
              <el-tag v-if="item.suspicious" type="warning" size="small">需核对</el-tag>
            </header>
            <el-alert
              v-if="item.suspicious && item.suspiciousReason"
              type="warning"
              :closable="false"
              show-icon
              :title="item.suspiciousReason"
              class="card-alert"
            />
            <el-form label-width="72px" size="small" class="card-form">
              <el-form-item label="标题">
                <el-input v-model="item.title" maxlength="128" show-word-limit />
              </el-form-item>
              <el-form-item label="题型">
                <el-select v-model="item.questionType" style="width: 160px">
                  <el-option
                    v-for="type in questionTypes"
                    :key="type"
                    :label="questionTypeLabel(type)"
                    :value="type"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="难度">
                <el-select v-model="item.difficulty" clearable placeholder="可选" style="width: 160px">
                  <el-option
                    v-for="diff in difficulties"
                    :key="diff"
                    :label="difficultyLabel(diff)"
                    :value="diff"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="题干">
                <el-input v-model="item.questionText" type="textarea" :rows="4" maxlength="20000" />
              </el-form-item>
              <el-form-item label="答案">
                <el-input v-model="item.answerText" type="textarea" :rows="2" maxlength="20000" />
              </el-form-item>
              <el-form-item label="解析">
                <el-input v-model="item.analysisText" type="textarea" :rows="2" maxlength="20000" />
              </el-form-item>
            </el-form>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.import-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: calc(100vh - 120px);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 600;
}

.steps {
  max-width: 720px;
  margin: 0 auto;
}

.upload-panel {
  max-width: 720px;
  margin: 24px auto 0;
  padding: 24px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.hint {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.hidden-input {
  display: none;
}

.review-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.review-toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.review-body {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(360px, 1fr);
  gap: 12px;
  min-height: 560px;
  flex: 1;
}

.pdf-pane,
.question-pane {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  min-height: 560px;
  overflow: hidden;
}

.pdf-frame {
  width: 100%;
  height: 100%;
  min-height: 560px;
  border: 0;
  background: #525659;
}

.question-pane {
  overflow: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
  background: var(--el-fill-color-blank);
}

.question-card.suspicious {
  border-color: var(--el-color-warning-light-5);
  background: var(--el-color-warning-light-9);
}

.question-card.discarded {
  opacity: 0.55;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.card-alert {
  margin-bottom: 8px;
}

.card-form :deep(.el-form-item) {
  margin-bottom: 10px;
}

@media (max-width: 1100px) {
  .review-body {
    grid-template-columns: 1fr;
  }

  .pdf-frame {
    min-height: 420px;
  }
}
</style>

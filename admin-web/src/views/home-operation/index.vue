<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/category'
import { contentApi } from '@/api/content'
import {
  createBanner,
  createRecommendation,
  deleteBanner,
  deleteRecommendation,
  disableBanner,
  enableBanner,
  getBanners,
  getPublicHome,
  getRecommendations,
  sortBanners,
  sortRecommendations,
  updateBanner,
} from '@/api/home'
import { topicApi } from '@/api/topic'
import ImageUpload from '@/components/ImageUpload.vue'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types/category'
import type { Content, ContentStatus, ContentType } from '@/types/content'
import type {
  BannerSaveRequest,
  HomeBanner,
  HomeItemStatus,
  HomeLinkType,
  HomeRecommendation,
  PublicHome,
  RecommendType,
} from '@/types/home'
import type { Topic, TopicStatus } from '@/types/topic'
import { categoryOptions, contentTypeLabel, formatTime, statusLabel } from '@/utils/content-form'
import { resolveMediaUrl } from '@/utils/media-url'

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const authStore = useAuthStore()
const canManage = computed(() => authStore.permissions.includes('HOME_OPERATION_MANAGE'))

const bannerLoading = ref(false)
const bannerError = ref('')
const banners = ref<HomeBanner[]>([])
const bannerTotal = ref(0)
const bannerDirty = ref(false)
const bannerSaving = ref(false)
const bannerQuery = reactive({
  pageNum: 1,
  pageSize: 50,
  keyword: '',
  status: '' as HomeItemStatus | '',
})

const recommendLoading = ref(false)
const recommendError = ref('')
const recommendations = ref<HomeRecommendation[]>([])
const recommendTotal = ref(0)
const recommendDirty = ref(false)
const recommendSaving = ref(false)
const recommendQuery = reactive({
  pageNum: 1,
  pageSize: 50,
  recommendType: '' as RecommendType | '',
  status: '' as HomeItemStatus | '',
})

const previewLoading = ref(false)
const previewError = ref('')
const preview = ref<PublicHome | null>(null)

const bannerDialog = ref(false)
const bannerSubmitting = ref(false)
const editingBannerId = ref('')
const pickedLabel = ref('')
const bannerForm = reactive({
  title: '',
  subtitle: '',
  imageUrl: '',
  linkType: 'NONE' as HomeLinkType,
  linkId: '',
  linkUrl: '',
  sort: 0,
  startTime: '',
  endTime: '',
})

const pickerOpen = ref(false)
const pickerMode = ref<'CONTENT' | 'TOPIC'>('CONTENT')
const pickerPurpose = ref<'banner' | 'recommend'>('banner')
const pickerLoading = ref(false)
const pickerError = ref('')
const contents = ref<Content[]>([])
const contentTotal = ref(0)
const topics = ref<Topic[]>([])
const topicTotal = ref(0)
const categories = ref<Category[]>([])
const contentQuery = reactive({
  pageNum: 1,
  pageSize: 8,
  keyword: '',
  categoryId: '',
  contentType: '' as ContentType | '',
  status: 'PUBLISHED' as ContentStatus | '',
})
const topicQuery = reactive({
  pageNum: 1,
  pageSize: 8,
  keyword: '',
  categoryId: '',
  status: 'PUBLISHED' as TopicStatus | '',
})

const categorySelect = computed(() => categoryOptions(categories.value))
const contentPicks = computed(() => preview.value?.recommendations.filter((item) => item.type === 'CONTENT') ?? [])
const topicPicks = computed(() => preview.value?.topics ?? [])

onMounted(() => {
  void loadCategories()
  void loadBanners()
  void loadRecommendations()
  void loadPreview()
})

async function loadCategories() {
  try {
    const response = await categoryApi.getCategoryTree()
    categories.value = response.data.data ?? []
  } catch {
    categories.value = []
  }
}

async function loadBanners() {
  bannerLoading.value = true
  bannerError.value = ''
  try {
    const response = await getBanners(bannerQuery)
    const page = response.data.data
    banners.value = page?.records ?? []
    bannerTotal.value = page?.total ?? 0
    bannerDirty.value = false
  } catch (error) {
    banners.value = []
    bannerTotal.value = 0
    bannerError.value = error instanceof Error ? error.message : 'Banner 加载失败'
  } finally {
    bannerLoading.value = false
  }
}

async function loadRecommendations() {
  recommendLoading.value = true
  recommendError.value = ''
  try {
    const response = await getRecommendations(recommendQuery)
    const page = response.data.data
    recommendations.value = page?.records ?? []
    recommendTotal.value = page?.total ?? 0
    recommendDirty.value = false
  } catch (error) {
    recommendations.value = []
    recommendTotal.value = 0
    recommendError.value = error instanceof Error ? error.message : '推荐加载失败'
  } finally {
    recommendLoading.value = false
  }
}

async function loadPreview() {
  previewLoading.value = true
  previewError.value = ''
  try {
    const response = await getPublicHome()
    preview.value = response.data.data
  } catch (error) {
    preview.value = null
    previewError.value = error instanceof Error ? error.message : '首页预览加载失败'
  } finally {
    previewLoading.value = false
  }
}

function openCreateBanner() {
  editingBannerId.value = ''
  pickedLabel.value = ''
  Object.assign(bannerForm, {
    title: '',
    subtitle: '',
    imageUrl: '',
    linkType: 'NONE',
    linkId: '',
    linkUrl: '',
    sort: banners.value.length,
    startTime: '',
    endTime: '',
  })
  bannerDialog.value = true
}

function openEditBanner(row: HomeBanner) {
  editingBannerId.value = row.id
  pickedLabel.value = row.targetTitle || ''
  Object.assign(bannerForm, {
    title: row.title,
    subtitle: row.subtitle || '',
    imageUrl: row.imageUrl,
    linkType: row.linkType,
    linkId: row.linkId || '',
    linkUrl: row.linkUrl || '',
    sort: row.sort,
    startTime: toPicker(row.startTime),
    endTime: toPicker(row.endTime),
  })
  bannerDialog.value = true
}

function onLinkTypeChange() {
  bannerForm.linkId = ''
  bannerForm.linkUrl = ''
  pickedLabel.value = ''
}

function bannerPayload(): BannerSaveRequest | null {
  const title = bannerForm.title.trim()
  if (!title) {
    ElMessage.warning('请填写标题')
    return null
  }
  if (!bannerForm.imageUrl.trim()) {
    ElMessage.warning('请上传 Banner 图片')
    return null
  }
  if (bannerForm.linkType === 'CONTENT' || bannerForm.linkType === 'TOPIC') {
    if (!bannerForm.linkId) {
      ElMessage.warning(bannerForm.linkType === 'CONTENT' ? '请选择内容' : '请选择专题')
      return null
    }
  }
  if (bannerForm.linkType === 'URL' && !/^https?:\/\/\S+$/i.test(bannerForm.linkUrl.trim())) {
    ElMessage.warning('请填写 http 或 https 链接')
    return null
  }
  if (bannerForm.startTime && bannerForm.endTime && bannerForm.endTime < bannerForm.startTime) {
    ElMessage.warning('结束时间不能早于开始时间')
    return null
  }
  return {
    title,
    subtitle: bannerForm.subtitle.trim(),
    imageUrl: bannerForm.imageUrl.trim(),
    linkType: bannerForm.linkType,
    linkId: bannerForm.linkType === 'CONTENT' || bannerForm.linkType === 'TOPIC' ? bannerForm.linkId : null,
    linkUrl: bannerForm.linkType === 'URL' ? bannerForm.linkUrl.trim() : null,
    sort: bannerForm.sort,
    startTime: bannerForm.startTime || null,
    endTime: bannerForm.endTime || null,
  }
}

async function submitBanner() {
  const payload = bannerPayload()
  if (!payload) {
    return
  }
  bannerSubmitting.value = true
  try {
    if (editingBannerId.value) {
      await updateBanner(editingBannerId.value, payload)
      ElMessage.success('已保存')
    } else {
      await createBanner(payload)
      ElMessage.success('已创建，默认停用')
    }
    bannerDialog.value = false
    await loadBanners()
    await loadPreview()
  } finally {
    bannerSubmitting.value = false
  }
}

async function onEnableBanner(row: HomeBanner) {
  await enableBanner(row.id)
  ElMessage.success('已启用')
  await loadBanners()
  await loadPreview()
}

async function onDisableBanner(row: HomeBanner) {
  await disableBanner(row.id)
  ElMessage.success('已停用')
  await loadBanners()
  await loadPreview()
}

async function onDeleteBanner(row: HomeBanner) {
  await ElMessageBox.confirm(`删除 Banner「${row.title}」？`, '删除 Banner', { type: 'warning' })
  await deleteBanner(row.id)
  ElMessage.success('已删除')
  await loadBanners()
  await loadPreview()
}

function moveBanner(index: number, delta: number) {
  swap(banners.value, index, index + delta)
  bannerDirty.value = true
}

async function saveBannerSort() {
  bannerSaving.value = true
  try {
    await sortBanners(banners.value.map((row, index) => ({ id: row.id, sort: index + 1 })))
    ElMessage.success('排序已保存')
    await loadBanners()
    await loadPreview()
  } finally {
    bannerSaving.value = false
  }
}

function moveRecommendation(index: number, delta: number) {
  swap(recommendations.value, index, index + delta)
  recommendDirty.value = true
}

async function saveRecommendationSort() {
  recommendSaving.value = true
  try {
    await sortRecommendations(recommendations.value.map((row, index) => ({ id: row.id, sort: index + 1 })))
    ElMessage.success('排序已保存')
    await loadRecommendations()
    await loadPreview()
  } finally {
    recommendSaving.value = false
  }
}

async function onDeleteRecommendation(row: HomeRecommendation) {
  await ElMessageBox.confirm(`移除推荐「${row.title || row.targetId}」？`, '移除推荐', { type: 'warning' })
  await deleteRecommendation(row.id)
  ElMessage.success('已移除')
  await loadRecommendations()
  await loadPreview()
}

function openContentPicker(purpose: 'banner' | 'recommend') {
  pickerPurpose.value = purpose
  pickerMode.value = 'CONTENT'
  contentQuery.pageNum = 1
  contentQuery.status = 'PUBLISHED'
  pickerOpen.value = true
  void searchContents()
}

function openTopicPicker(purpose: 'banner' | 'recommend') {
  pickerPurpose.value = purpose
  pickerMode.value = 'TOPIC'
  topicQuery.pageNum = 1
  topicQuery.status = 'PUBLISHED'
  pickerOpen.value = true
  void searchTopics()
}

function openRecommendPicker() {
  openContentPicker('recommend')
}

async function searchContents() {
  pickerLoading.value = true
  pickerError.value = ''
  try {
    const response = await contentApi.getContentList({
      pageNum: contentQuery.pageNum,
      pageSize: contentQuery.pageSize,
      keyword: contentQuery.keyword,
      categoryId: contentQuery.categoryId,
      contentType: contentQuery.contentType,
      status: contentQuery.status,
    })
    const page = response.data.data
    contents.value = page?.records ?? []
    contentTotal.value = page?.total ?? 0
  } catch (error) {
    contents.value = []
    contentTotal.value = 0
    pickerError.value = error instanceof Error ? error.message : '内容加载失败'
  } finally {
    pickerLoading.value = false
  }
}

async function searchTopics() {
  pickerLoading.value = true
  pickerError.value = ''
  try {
    const response = await topicApi.getTopicList({
      pageNum: topicQuery.pageNum,
      pageSize: topicQuery.pageSize,
      keyword: topicQuery.keyword,
      categoryId: topicQuery.categoryId,
      status: topicQuery.status,
    })
    const page = response.data.data
    topics.value = page?.records ?? []
    topicTotal.value = page?.total ?? 0
  } catch (error) {
    topics.value = []
    topicTotal.value = 0
    pickerError.value = error instanceof Error ? error.message : '专题加载失败'
  } finally {
    pickerLoading.value = false
  }
}

function chooseContent(row: Content) {
  if (pickerPurpose.value === 'banner') {
    bannerForm.linkType = 'CONTENT'
    bannerForm.linkId = row.id
    pickedLabel.value = row.title
    pickerOpen.value = false
    return
  }
  void addRecommendation('CONTENT', row.id)
}

function chooseTopic(row: Topic) {
  if (pickerPurpose.value === 'banner') {
    bannerForm.linkType = 'TOPIC'
    bannerForm.linkId = row.id
    pickedLabel.value = row.name
    pickerOpen.value = false
    return
  }
  void addRecommendation('TOPIC', row.id)
}

async function addRecommendation(type: RecommendType, targetId: string) {
  const maxSort = recommendations.value.reduce((max, row) => Math.max(max, row.sort), 0)
  await createRecommendation({ recommendType: type, targetId, sort: maxSort + 1 })
  ElMessage.success('已加入重点推荐')
  pickerOpen.value = false
  await loadRecommendations()
  await loadPreview()
}

function linkTypeLabel(type: HomeLinkType) {
  if (type === 'CONTENT') return '内容'
  if (type === 'TOPIC') return '专题'
  if (type === 'URL') return '链接'
  return '不跳转'
}

function linkTarget(row: HomeBanner) {
  if (row.linkType === 'NONE') return '—'
  if (row.linkType === 'URL') return row.linkUrl || '—'
  return row.targetTitle || row.linkId || '—'
}

function recommendTypeLabel(row: HomeRecommendation) {
  if (row.recommendType === 'TOPIC') return '专题'
  return row.contentType ? contentTypeLabel(row.contentType) : '内容'
}

function publishLabel(status: ContentStatus | TopicStatus) {
  return statusLabel(status as ContentStatus)
}

function onPickerMode(mode: string | number | boolean | undefined) {
  if (mode === 'TOPIC') {
    void searchTopics()
    return
  }
  void searchContents()
}

function targetStatus(row: HomeRecommendation) {
  const status = row.recommendType === 'TOPIC' ? row.topicStatus : row.contentStatus
  if (status === 'PUBLISHED' || status === 'DRAFT' || status === 'OFFLINE') {
    return statusLabel(status)
  }
  return '目标不可用'
}

function media(url: string | null | undefined) {
  return url ? resolveMediaUrl(url, apiBase) : ''
}

function toPicker(value: string | null) {
  if (!value) return ''
  return value.replace(' ', 'T').slice(0, 19)
}

function swap<T>(list: T[], from: number, to: number) {
  if (to < 0 || to >= list.length) return
  const next = list[to]
  list[to] = list[from]
  list[from] = next
}
</script>

<template>
  <section class="block">
    <div class="block-head">
      <h2>Banner 管理</h2>
      <div class="actions">
        <el-button v-if="canManage" :disabled="!bannerDirty" :loading="bannerSaving" @click="saveBannerSort">
          保存排序
        </el-button>
        <el-button v-if="canManage" type="primary" @click="openCreateBanner">新增 Banner</el-button>
      </div>
    </div>
    <p class="hint">建议图片 750 × 300。新建后默认停用，启用后才会出现在首页。首页最多展示 5 个有效 Banner。当前 {{ bannerTotal }} 条。</p>
    <el-form inline @submit.prevent="loadBanners">
      <el-form-item label="标题">
        <el-input v-model="bannerQuery.keyword" clearable placeholder="关键字" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="bannerQuery.status" clearable placeholder="全部" style="width: 140px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="bannerQuery.pageNum = 1; loadBanners()">查询</el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="bannerError" :title="bannerError" type="error" show-icon />
    <el-table v-loading="bannerLoading" :data="banners" empty-text="暂无 Banner">
      <el-table-column label="封面" width="140">
        <template #default="{ row }">
          <el-image class="cover" :src="media(row.imageUrl)" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="140" />
      <el-table-column label="跳转类型" width="100">
        <template #default="{ row }">{{ linkTypeLabel(row.linkType) }}</template>
      </el-table-column>
      <el-table-column label="跳转目标" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ linkTarget(row) }}</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="170">
        <template #default="{ row }">{{ formatTime(row.startTime) || '—' }}</template>
      </el-table-column>
      <el-table-column label="结束时间" width="170">
        <template #default="{ row }">{{ formatTime(row.endTime) || '—' }}</template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" width="280" fixed="right">
        <template #default="{ row, $index }">
          <el-button link type="primary" @click="openEditBanner(row)">编辑</el-button>
          <el-button v-if="row.status !== 'ENABLED'" link type="primary" @click="onEnableBanner(row)">启用</el-button>
          <el-button v-else link type="primary" @click="onDisableBanner(row)">停用</el-button>
          <el-button link type="danger" @click="onDeleteBanner(row)">删除</el-button>
          <el-button link :disabled="$index === 0" @click="moveBanner($index, -1)">上移</el-button>
          <el-button link :disabled="$index === banners.length - 1" @click="moveBanner($index, 1)">下移</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>

  <section class="block">
    <div class="block-head">
      <h2>重点推荐</h2>
      <div class="actions">
        <el-button v-if="canManage" :disabled="!recommendDirty" :loading="recommendSaving" @click="saveRecommendationSort">
          保存排序
        </el-button>
        <el-button v-if="canManage" type="primary" @click="openRecommendPicker">添加推荐</el-button>
      </div>
    </div>
    <p class="hint">内容与专题共用重点推荐。已发布的专题会同时出现在首页「专题精选」。下线或删除后前台自动隐藏，重新发布后恢复。首页最多展示 6 条。当前 {{ recommendTotal }} 条。</p>
    <el-form inline>
      <el-form-item label="类型">
        <el-select v-model="recommendQuery.recommendType" clearable placeholder="全部" style="width: 140px">
          <el-option label="内容" value="CONTENT" />
          <el-option label="专题" value="TOPIC" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="recommendQuery.status" clearable placeholder="全部" style="width: 140px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="recommendQuery.pageNum = 1; loadRecommendations()">查询</el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="recommendError" :title="recommendError" type="error" show-icon />
    <el-table v-loading="recommendLoading" :data="recommendations" empty-text="暂无推荐">
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ recommendTypeLabel(row) }}</template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column label="封面" width="100">
        <template #default="{ row }">
          <el-image v-if="row.coverUrl" class="thumb" :src="media(row.coverUrl)" fit="cover" />
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="所属分类" min-width="120" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">{{ targetStatus(row) }}</template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" width="180" fixed="right">
        <template #default="{ row, $index }">
          <el-button link type="danger" @click="onDeleteRecommendation(row)">移除</el-button>
          <el-button link :disabled="$index === 0" @click="moveRecommendation($index, -1)">上移</el-button>
          <el-button link :disabled="$index === recommendations.length - 1" @click="moveRecommendation($index, 1)">
            下移
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>

  <section class="block">
    <div class="block-head">
      <h2>首页预览</h2>
      <el-button :loading="previewLoading" @click="loadPreview">刷新预览</el-button>
    </div>
    <p class="hint">预览读取公开接口 GET /api/home，与小程序首页使用同一份数据。</p>
    <el-alert v-if="previewError" :title="previewError" type="error" show-icon />
    <div v-else-if="preview" class="phone">
      <div v-if="preview.banners.length" class="phone-banner">
        <img :src="media(preview.banners[0].imageUrl)" alt="" />
        <div class="phone-banner-text">
          <strong>{{ preview.banners[0].title }}</strong>
          <span v-if="preview.banners[0].subtitle">{{ preview.banners[0].subtitle }}</span>
        </div>
      </div>
      <div v-if="preview.categories.length" class="phone-cats">
        <span v-for="item in preview.categories" :key="item.id">{{ item.name }}</span>
      </div>
      <template v-if="contentPicks.length">
        <h3>重点推荐</h3>
        <p v-for="item in contentPicks" :key="item.id">{{ item.title }}</p>
      </template>
      <template v-if="topicPicks.length">
        <h3>专题精选</h3>
        <p v-for="item in topicPicks" :key="item.id">{{ item.title }}</p>
      </template>
      <h3>最新内容</h3>
      <p v-if="!preview.latestContents.length" class="muted">暂无内容</p>
      <p v-for="item in preview.latestContents" :key="item.id">{{ item.title }}</p>
    </div>
  </section>

  <el-dialog v-model="bannerDialog" :title="editingBannerId ? '编辑 Banner' : '新增 Banner'" width="640px">
    <el-form label-width="96px">
      <el-form-item label="标题" required>
        <el-input v-model="bannerForm.title" maxlength="200" />
      </el-form-item>
      <el-form-item label="副标题">
        <el-input v-model="bannerForm.subtitle" maxlength="500" />
      </el-form-item>
      <el-form-item label="Banner 图片" required>
        <ImageUpload v-model="bannerForm.imageUrl" />
        <span class="hint">建议 750 × 300，不强制尺寸。</span>
      </el-form-item>
      <el-form-item label="跳转类型" required>
        <el-select v-model="bannerForm.linkType" @change="onLinkTypeChange">
          <el-option label="不跳转" value="NONE" />
          <el-option label="内容" value="CONTENT" />
          <el-option label="专题" value="TOPIC" />
          <el-option label="链接" value="URL" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="bannerForm.linkType === 'CONTENT'" label="选择内容" required>
        <el-button @click="openContentPicker('banner')">选择内容</el-button>
        <span class="picked">{{ pickedLabel || bannerForm.linkId }}</span>
      </el-form-item>
      <el-form-item v-if="bannerForm.linkType === 'TOPIC'" label="选择专题" required>
        <el-button @click="openTopicPicker('banner')">选择专题</el-button>
        <span class="picked">{{ pickedLabel || bannerForm.linkId }}</span>
      </el-form-item>
      <el-form-item v-if="bannerForm.linkType === 'URL'" label="URL" required>
        <el-input v-model="bannerForm.linkUrl" placeholder="https://" />
        <span class="hint">小程序只能打开已配置的业务域名，其他链接会提示复制。</span>
      </el-form-item>
      <el-form-item label="排序" required>
        <el-input-number v-model="bannerForm.sort" :min="0" :max="9999" />
      </el-form-item>
      <el-form-item label="开始时间">
        <el-date-picker
          v-model="bannerForm.startTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="留空表示立即"
        />
      </el-form-item>
      <el-form-item label="结束时间">
        <el-date-picker
          v-model="bannerForm.endTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="留空表示不结束"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="bannerDialog = false">取消</el-button>
      <el-button type="primary" :loading="bannerSubmitting" @click="submitBanner">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="pickerOpen" :title="pickerMode === 'CONTENT' ? '选择内容' : '选择专题'" width="860px">
    <el-radio-group
      v-if="pickerPurpose === 'recommend'"
      v-model="pickerMode"
      @change="onPickerMode"
    >
      <el-radio-button value="CONTENT">内容</el-radio-button>
      <el-radio-button value="TOPIC">专题</el-radio-button>
    </el-radio-group>
    <el-form v-if="pickerMode === 'CONTENT'" inline class="picker-form">
      <el-input v-model="contentQuery.keyword" placeholder="标题" clearable style="width: 180px" />
      <el-select v-model="contentQuery.contentType" clearable placeholder="类型" style="width: 140px">
        <el-option label="文章" value="ARTICLE" />
        <el-option label="视频" value="VIDEO" />
        <el-option label="题目" value="QUESTION" />
        <el-option label="每周一题" value="WEEKLY" />
        <el-option label="资料" value="DOCUMENT" />
      </el-select>
      <el-select v-model="contentQuery.categoryId" clearable placeholder="分类" style="width: 180px">
        <el-option v-for="item in categorySelect" :key="item.id" :label="item.label" :value="item.id" />
      </el-select>
      <el-select v-model="contentQuery.status" clearable placeholder="状态" style="width: 140px">
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已下线" value="OFFLINE" />
      </el-select>
      <el-button type="primary" @click="contentQuery.pageNum = 1; searchContents()">搜索</el-button>
    </el-form>
    <el-form v-else inline class="picker-form">
      <el-input v-model="topicQuery.keyword" placeholder="专题名称" clearable style="width: 220px" />
      <el-select v-model="topicQuery.status" clearable placeholder="状态" style="width: 140px">
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已下线" value="OFFLINE" />
      </el-select>
      <el-button type="primary" @click="topicQuery.pageNum = 1; searchTopics()">搜索</el-button>
    </el-form>
    <el-alert v-if="pickerError" :title="pickerError" type="error" show-icon />
    <el-table v-if="pickerMode === 'CONTENT'" v-loading="pickerLoading" :data="contents" empty-text="暂无内容">
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ contentTypeLabel(row.contentType) }}</template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">{{ publishLabel(row.status) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="chooseContent(row)">选择</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-table v-else v-loading="pickerLoading" :data="topics" empty-text="暂无专题">
      <el-table-column prop="name" label="专题名称" min-width="200" />
      <el-table-column prop="categoryName" label="分类" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">{{ publishLabel(row.status) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="chooseTopic(row)">选择</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<style scoped>
.block {
  margin-bottom: 28px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.block-head h2,
.phone h3 {
  margin: 0;
  font-size: 16px;
}

.actions {
  display: flex;
  gap: 8px;
}

.hint,
.muted {
  color: #8f959e;
  font-size: 13px;
}

.cover {
  width: 120px;
  height: 48px;
  border-radius: 4px;
}

.thumb {
  width: 64px;
  height: 40px;
  border-radius: 4px;
}

.picked {
  margin-left: 12px;
}

.picker-form {
  margin: 12px 0;
}

.phone {
  width: 375px;
  max-width: 100%;
  padding: 16px;
  border: 1px solid #e6e8eb;
  border-radius: 24px;
  background: #f7f8fa;
}

.phone-banner {
  position: relative;
  overflow: hidden;
  height: 150px;
  border-radius: 12px;
  background: #d8dee6;
}

.phone-banner img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.phone-banner-text {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 12px;
  display: flex;
  flex-direction: column;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.45);
}

.phone-cats {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.phone-cats span {
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
}

.phone p {
  margin: 8px 0 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff;
}
</style>

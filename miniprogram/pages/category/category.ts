import { getCategoryDetail, getCategoryTree } from '../../services/category'
import { getContentList } from '../../services/content'
import { PageStatus } from '../../types/api'
import { CategoryTreeVO } from '../../types/category'
import { CategoryChildLink, CategoryPageModel, RequestFailure, isCategoryId, resolveCategoryPage } from '../../utils/category-tree'
import { LatestItem, buildLatestItems, buildLatestQuery } from '../../utils/content-view'
import { ApiError, toErrorMessage } from '../../utils/error'

interface CategoryPageData {
  status: PageStatus
  message: string
  name: string
  categoryId: string
  children: CategoryChildLink[]
  tree: CategoryTreeVO[]
  latest: LatestItem[]
  latestStatus: PageStatus
  latestError: string
  loading: boolean
}

Page<CategoryPageData, WechatMiniprogram.IAnyObject>({
  data: {
    status: 'loading',
    message: '',
    name: '',
    categoryId: '',
    children: [],
    tree: [],
    latest: [],
    latestStatus: 'loading',
    latestError: '',
    loading: false
  },

  onLoad(query) {
    this.setData({ categoryId: query.id || '' })
    this.loadCategory()
  },

  onPullDownRefresh() {
    this.loadCategory().finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  loadCategory() {
    const categoryId = this.data.categoryId.trim()
    if (!categoryId || !isCategoryId(categoryId)) {
      this.applyModel(resolveCategoryPage({
        rawId: categoryId,
        detailName: null,
        tree: null,
        failure: null
      }))
      this.setData({ latest: [], latestStatus: 'empty', latestError: '' })
      return Promise.resolve()
    }
    if (this.data.loading) {
      return Promise.resolve()
    }
    this.data.loading = true
    this.setData({ status: 'loading', message: '', latestStatus: 'loading', latestError: '', loading: true })
    return Promise.all([getCategoryDetail(categoryId), getCategoryTree()])
      .then(([detail, tree]) => {
        this.applyModel(resolveCategoryPage({
          rawId: categoryId,
          detailName: detail.name,
          tree,
          failure: null
        }))
        this.setData({ tree })
        return this.loadLatest(categoryId, tree)
      })
      .catch((error: unknown) => {
        this.applyModel(resolveCategoryPage({
          rawId: categoryId,
          detailName: null,
          tree: null,
          failure: toFailure(error)
        }))
        this.setData({ latest: [], latestStatus: 'empty', latestError: '' })
      })
      .finally(() => {
        this.data.loading = false
        this.setData({ loading: false })
      })
  },

  loadLatest(categoryId: string, tree: CategoryTreeVO[]) {
    return getContentList(buildLatestQuery(categoryId))
      .then((page) => {
        const records = page.records || []
        this.setData({
          latest: buildLatestItems(records, tree),
          latestStatus: records.length === 0 ? 'empty' : 'success'
        })
      })
      .catch((error: unknown) => {
        this.setData({
          latestStatus: 'error',
          latestError: toErrorMessage(error, '内容加载失败，请稍后重试')
        })
      })
  },

  applyModel(model: CategoryPageModel) {
    this.setData({
      status: model.status,
      message: model.message,
      name: model.name,
      children: model.children
    })
    if (model.name) {
      wx.setNavigationBarTitle({ title: model.name })
    }
  }
})

function toFailure(error: unknown): RequestFailure {
  if (error instanceof ApiError) {
    return {
      code: error.code,
      httpStatus: error.httpStatus,
      message: toErrorMessage(error, '分类加载失败，请稍后重试')
    }
  }
  return {
    code: 'NETWORK',
    httpStatus: null,
    message: toErrorMessage(error, '分类加载失败，请稍后重试')
  }
}

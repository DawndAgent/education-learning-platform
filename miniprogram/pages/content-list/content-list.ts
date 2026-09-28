import { getCategoryDetail } from '../../services/category'
import { getContentList } from '../../services/content'
import { ContentType } from '../../types/content'
import {
  ListSession,
  buildCategoryContentQuery,
  commitPage,
  createSession,
  failLoad,
  failMore,
  invalidParamSession,
  loadFailureMessage,
  resolveCategoryParam,
  startFirst,
  startMore,
  startRefresh
} from '../../utils/content-list'

const CONTENT_TYPES: ContentType[] = ['ARTICLE', 'VIDEO', 'QUESTION', 'TOPIC', 'WEEKLY', 'DOCUMENT']

interface ContentListPageData extends ListSession {
  categoryId: string
  categoryName: string
  contentType: string
}

Page<ContentListPageData, WechatMiniprogram.IAnyObject>({
  data: {
    ...createSession(),
    categoryId: '',
    categoryName: '',
    contentType: ''
  },

  onLoad(query) {
    const contentType = readContentType(query.contentType) || ''
    const param = resolveCategoryParam(query.categoryId || '')
    if (!param.ok) {
      this.apply(invalidParamSession(param.message))
      this.setData({ categoryId: '', contentType })
      return
    }
    this.setData({ categoryId: param.categoryId, contentType })
    this.data.categoryId = param.categoryId
    this.data.contentType = contentType
    this.loadFirst()
  },

  onPullDownRefresh() {
    if (!resolveCategoryParam(this.data.categoryId).ok) {
      wx.stopPullDownRefresh()
      return
    }
    const started = startRefresh(this.session())
    this.apply(started.session)
    this.fetchPage(started.generation, 1, 'replace').finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  onReachBottom() {
    const nextPage = this.data.pageNum + 1
    const started = startMore(this.session())
    if (!started) {
      return
    }
    this.apply(started.session)
    this.fetchPage(started.generation, nextPage, 'append')
  },

  loadFirst() {
    if (!resolveCategoryParam(this.data.categoryId).ok) {
      return
    }
    const started = startFirst(this.session())
    this.apply(started.session)
    this.fetchPage(started.generation, 1, 'replace')
  },

  goBack() {
    wx.navigateBack({
      fail() {
        wx.redirectTo({ url: '/pages/index/index' })
      }
    })
  },

  fetchPage(generation: number, pageNum: number, mode: 'replace' | 'append') {
    const categoryId = this.data.categoryId
    const contentType = readContentType(this.data.contentType)
    const query = buildCategoryContentQuery(categoryId, pageNum, contentType)
    const detail = pageNum === 1 ? getCategoryDetail(categoryId) : Promise.resolve(null)
    return Promise.all([detail, getContentList(query)])
      .then(([category, page]) => {
        if (this.data.generation !== generation) {
          return
        }
        const next = commitPage(this.session(), generation, page, mode)
        this.apply(next)
        if (category) {
          this.setData({ categoryName: category.name })
        }
      })
      .catch((error: unknown) => {
        if (this.data.generation !== generation) {
          return
        }
        if (mode === 'append') {
          this.apply(failMore(this.session(), generation))
          return
        }
        this.apply(failLoad(this.session(), generation, loadFailureMessage(error)))
      })
  },

  apply(session: ListSession) {
    Object.assign(this.data, session)
    this.setData(session)
  },

  session(): ListSession {
    return {
      generation: this.data.generation,
      loading: this.data.loading,
      refreshing: this.data.refreshing,
      loadingMore: this.data.loadingMore,
      status: this.data.status,
      message: this.data.message,
      moreError: this.data.moreError,
      canRetry: this.data.canRetry,
      items: this.data.items,
      pageNum: this.data.pageNum,
      pageSize: this.data.pageSize,
      total: this.data.total,
      hasMore: this.data.hasMore
    }
  }
})

function readContentType(value: string | undefined): ContentType | undefined {
  if (CONTENT_TYPES.some((item) => item === value)) {
    return value as ContentType
  }
  return undefined
}

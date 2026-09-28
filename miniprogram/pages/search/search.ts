import { getContentList } from '../../services/content'
import {
  ListSession,
  commitPage,
  createSession,
  failLoad,
  failMore,
  startFirst,
  startMore,
  startRefresh
} from '../../utils/content-list'
import { buildSearchQuery } from '../../utils/content-view'
import { toErrorMessage } from '../../utils/error'
import { clearSearchHistory, pushSearchHistory, readSearchHistory } from '../../utils/search-history'

interface SearchPageData extends ListSession {
  keyword: string
  inputValue: string
  history: string[]
  searched: boolean
}

Page<SearchPageData, WechatMiniprogram.IAnyObject>({
  data: {
    ...createSession(),
    keyword: '',
    inputValue: '',
    history: [],
    searched: false,
    status: 'empty',
    message: '请输入搜索关键词'
  },

  onLoad(query) {
    const keyword = (query.keyword || '').trim()
    this.setData({
      history: readSearchHistory(),
      inputValue: keyword,
      keyword
    })
    if (keyword) {
      this.runSearch(keyword, 'first')
    }
  },

  onPullDownRefresh() {
    if (!this.data.keyword.trim()) {
      wx.stopPullDownRefresh()
      return
    }
    this.runSearch(this.data.keyword, 'refresh').finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  onReachBottom() {
    if (!this.data.keyword.trim()) {
      return
    }
    const nextPage = this.data.pageNum + 1
    const started = startMore(this.session())
    if (!started) {
      return
    }
    this.apply(started.session)
    this.fetchPage(started.generation, nextPage, 'append')
  },

  onInput(event: WechatMiniprogram.Input) {
    this.setData({ inputValue: event.detail.value || '' })
  },

  onConfirm() {
    this.submitSearch(this.data.inputValue)
  },

  onSearchTap() {
    this.submitSearch(this.data.inputValue)
  },

  onHistoryTap(event: WechatMiniprogram.TouchEvent) {
    const keyword = String(event.currentTarget.dataset.keyword || '')
    this.setData({ inputValue: keyword })
    this.submitSearch(keyword)
  },

  onClearHistory() {
    clearSearchHistory()
    this.setData({ history: [] })
  },

  retrySearch() {
    if (!this.data.keyword.trim()) {
      return
    }
    this.runSearch(this.data.keyword, 'first')
  },

  submitSearch(raw: string) {
    const keyword = raw.trim()
    if (!keyword) {
      this.setData({
        ...createSession(),
        keyword: '',
        inputValue: '',
        searched: false,
        status: 'empty',
        message: '请输入搜索关键词',
        history: readSearchHistory()
      })
      return
    }
    this.setData({
      inputValue: keyword,
      keyword,
      history: pushSearchHistory(keyword)
    })
    this.runSearch(keyword, 'first')
  },

  runSearch(keyword: string, mode: 'first' | 'refresh') {
    this.data.keyword = keyword
    const started = mode === 'refresh' ? startRefresh(this.session()) : startFirst(this.session())
    this.apply({
      ...started.session,
      searched: true,
      message: ''
    })
    return this.fetchPage(started.generation, 1, 'replace')
  },

  fetchPage(generation: number, pageNum: number, mode: 'replace' | 'append') {
    const keyword = this.data.keyword.trim()
    return getContentList(buildSearchQuery(keyword, pageNum))
      .then((page) => {
        if (this.data.generation !== generation) {
          return
        }
        const next = commitPage(this.session(), generation, page, mode)
        if (next.status === 'empty') {
          next.message = '暂无相关内容'
        }
        this.apply({ ...next, searched: true })
      })
      .catch((error: unknown) => {
        if (this.data.generation !== generation) {
          return
        }
        if (mode === 'append') {
          this.apply(failMore(this.session(), generation))
          return
        }
        this.apply(
          failLoad(this.session(), generation, toErrorMessage(error, '搜索失败，请稍后重试'))
        )
      })
  },

  apply(partial: Partial<SearchPageData> & ListSession) {
    Object.assign(this.data, partial)
    this.setData(partial)
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

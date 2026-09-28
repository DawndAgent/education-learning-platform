import { getTopicList } from '../../services/topic'
import { PageStatus } from '../../types/api'
import type { TopicListItem } from '../../types/topic'
import { toErrorMessage } from '../../utils/error'
import { topicDetailUrl } from '../../utils/topic-view'

interface TopicCard {
  id: string
  name: string
  coverUrl: string
  summary: string
  meta: string
  url: string
}

interface TopicListPageData {
  status: PageStatus
  message: string
  canRetry: boolean
  loadingMore: boolean
  moreError: string
  hasMore: boolean
  pageNum: number
  total: number
  cards: TopicCard[]
  loading: boolean
  generation: number
}

const PAGE_SIZE = 10

Page<TopicListPageData, WechatMiniprogram.IAnyObject>({
  data: {
    status: 'loading',
    message: '',
    canRetry: true,
    loadingMore: false,
    moreError: '',
    hasMore: false,
    pageNum: 1,
    total: 0,
    cards: [],
    loading: false,
    generation: 0
  },

  onLoad() {
    this.loadFirst()
  },

  onPullDownRefresh() {
    this.loadFirst().finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  onReachBottom() {
    if (this.data.loading || this.data.loadingMore || !this.data.hasMore || this.data.status !== 'success') {
      return
    }
    this.setData({ loadingMore: true, moreError: '' })
    this.fetchPage(this.data.generation, this.data.pageNum + 1, 'append')
  },

  loadFirst() {
    if (this.data.loading) {
      return Promise.resolve()
    }
    const generation = this.data.generation + 1
    this.data.generation = generation
    this.data.loading = true
    this.setData({
      generation,
      loading: true,
      status: this.data.cards.length === 0 ? 'loading' : this.data.status,
      message: '',
      moreError: '',
      loadingMore: false
    })
    return this.fetchPage(generation, 1, 'replace').finally(() => {
      this.data.loading = false
      this.setData({ loading: false })
    })
  },

  fetchPage(generation: number, pageNum: number, mode: 'replace' | 'append') {
    return getTopicList({ pageNum, pageSize: PAGE_SIZE })
      .then((page) => {
        if (this.data.generation !== generation) {
          return
        }
        const incoming = (page.records || []).map(toCard)
        const cards = mode === 'replace' ? incoming : mergeCards(this.data.cards, incoming)
        const stalled = mode === 'append' && cards.length === this.data.cards.length
        const hasMore = !stalled && cards.length < page.total
        this.setData({
          cards,
          pageNum: page.pageNum,
          total: page.total,
          hasMore,
          loadingMore: false,
          moreError: '',
          status: cards.length === 0 ? 'empty' : 'success',
          message: cards.length === 0 ? '暂无专题' : '',
          canRetry: true
        })
      })
      .catch((error: unknown) => {
        if (this.data.generation !== generation) {
          return
        }
        if (mode === 'append') {
          this.setData({ loadingMore: false, moreError: '加载更多失败，请重试' })
          return
        }
        this.setData({
          loadingMore: false,
          status: 'error',
          message: toErrorMessage(error, '专题加载失败，请稍后重试'),
          canRetry: true
        })
      })
  }
})

function toCard(item: TopicListItem): TopicCard {
  return {
    id: item.id,
    name: item.name,
    coverUrl: item.coverUrl || '',
    summary: item.summary || '',
    meta: item.categoryName || '',
    url: topicDetailUrl(item.id)
  }
}

function mergeCards(existing: TopicCard[], incoming: TopicCard[]): TopicCard[] {
  const seen = new Set(existing.map((item) => item.id))
  const next = existing.slice()
  incoming.forEach((item) => {
    if (seen.has(item.id)) {
      return
    }
    seen.add(item.id)
    next.push(item)
  })
  return next
}

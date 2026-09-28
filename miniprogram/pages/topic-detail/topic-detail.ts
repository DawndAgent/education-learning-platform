import { getTopicDetail } from '../../services/topic'
import { PageStatus } from '../../types/api'
import { ApiError, toErrorMessage } from '../../utils/error'
import { contentDetailUrl } from '../../utils/content-view'
import { topicTypeLabel } from '../../utils/topic-view'

interface TopicContentCard {
  id: string
  index: number
  title: string
  coverUrl: string
  summary: string
  typeLabel: string
  url: string
}

interface TopicDetailPageData {
  status: PageStatus
  message: string
  canRetry: boolean
  topicId: string
  name: string
  coverUrl: string
  summary: string
  categoryName: string
  contents: TopicContentCard[]
  generation: number
}

Page<TopicDetailPageData, WechatMiniprogram.IAnyObject>({
  data: {
    status: 'loading',
    message: '',
    canRetry: true,
    topicId: '',
    name: '',
    coverUrl: '',
    summary: '',
    categoryName: '',
    contents: [],
    generation: 0
  },

  onLoad(query) {
    const topicId = (query.id || '').trim()
    this.setData({ topicId })
    this.data.topicId = topicId
    this.loadDetail()
  },

  loadDetail() {
    const topicId = this.data.topicId
    if (!topicId || !/^\d{1,20}$/.test(topicId)) {
      this.setData({
        status: 'error',
        message: '专题参数无效',
        canRetry: false
      })
      return
    }
    const generation = this.data.generation + 1
    this.data.generation = generation
    this.setData({
      generation,
      status: 'loading',
      message: '',
      canRetry: true
    })
    getTopicDetail(topicId)
      .then((detail) => {
        if (this.data.generation !== generation) {
          return
        }
        const contents = (detail.contents || []).map((item, index) => ({
          id: item.id,
          index: index + 1,
          title: item.title,
          coverUrl: item.coverUrl || '',
          summary: item.summary || '',
          typeLabel: topicTypeLabel(item.contentType),
          url: contentDetailUrl(item.id)
        }))
        this.setData({
          status: 'success',
          name: detail.name,
          coverUrl: detail.coverUrl || '',
          summary: detail.summary || '',
          categoryName: detail.category?.name || '',
          contents
        })
        wx.setNavigationBarTitle({ title: detail.name || '专题详情' })
      })
      .catch((error: unknown) => {
        if (this.data.generation !== generation) {
          return
        }
        const notFound = error instanceof ApiError && (error.code === '404' || error.httpStatus === 404)
        this.setData({
          status: 'error',
          message: notFound ? '专题不存在或已下线' : toErrorMessage(error, '专题加载失败，请稍后重试'),
          canRetry: !notFound || true
        })
      })
  },

  goBack() {
    wx.navigateBack({
      fail() {
        wx.redirectTo({ url: '/pages/topic-list/topic-list' })
      }
    })
  }
})

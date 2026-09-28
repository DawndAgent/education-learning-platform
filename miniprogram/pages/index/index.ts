import { getHome } from '../../services/home'
import { PageStatus } from '../../types/api'
import { toErrorMessage } from '../../utils/error'
import { HomeEntry } from '../../utils/category-tree'
import { LatestItem, searchPageUrl } from '../../utils/content-view'
import { BannerCard, RecommendCard, toBannerCards, toCategoryEntries, toContentRecommendations, toLatestCards, toTopicRecommendations } from '../../utils/home-view'
import { topicListUrl } from '../../utils/topic-view'

interface IndexPageData {
  status: PageStatus
  error: string
  banners: BannerCard[]
  entries: HomeEntry[]
  recommendations: RecommendCard[]
  topics: RecommendCard[]
  latest: LatestItem[]
  topicListUrl: string
  loading: boolean
}

Page<IndexPageData, WechatMiniprogram.IAnyObject>({
  data: {
    status: 'loading',
    error: '',
    banners: [],
    entries: [],
    recommendations: [],
    topics: [],
    latest: [],
    topicListUrl: topicListUrl(),
    loading: false
  },

  onLoad() {
    this.reload()
  },

  onPullDownRefresh() {
    this.reload().finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  goSearch() {
    wx.navigateTo({ url: searchPageUrl() })
  },

  goMore() {
    wx.navigateTo({ url: searchPageUrl() })
  },

  goTopicList() {
    wx.navigateTo({ url: topicListUrl() })
  },

  onBannerTap(event: WechatMiniprogram.TouchEvent) {
    const index = Number(event.currentTarget.dataset.index)
    const banner = this.data.banners[index]
    if (!banner || banner.action === 'none') {
      return
    }
    if (banner.action === 'url') {
      wx.setClipboardData({
        data: banner.linkUrl,
        success() {
          wx.showToast({
            title: '链接已复制，需配置小程序业务域名后才能打开',
            icon: 'none'
          })
        }
      })
      return
    }
    wx.navigateTo({ url: banner.url })
  },

  reload() {
    if (this.data.loading) {
      return Promise.resolve()
    }
    this.data.loading = true
    this.setData({ loading: true, status: 'loading', error: '' })
    return getHome()
      .then((page) => {
        const latest = toLatestCards(page.latestContents || [])
        this.setData({
          banners: toBannerCards(page.banners || []),
          entries: toCategoryEntries(page.categories || []),
          recommendations: toContentRecommendations(page.recommendations || []),
          topics: toTopicRecommendations(page.topics || []),
          latest,
          status: 'success',
          error: ''
        })
      })
      .catch((error: unknown) => {
        this.setData({
          status: 'error',
          error: toErrorMessage(error, '首页加载失败')
        })
      })
      .finally(() => {
        this.data.loading = false
        this.setData({ loading: false })
      })
  }
})

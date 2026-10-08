import { getCategoryTree } from '../../services/category'
import { getContentList } from '../../services/content'
import { getHome } from '../../services/home'
import { PageStatus } from '../../types/api'
import { CategoryTreeVO } from '../../types/category'
import {
  CategoryChildLink,
  findCategory,
  listChildren,
  contentListUrl
} from '../../utils/category-tree'
import { LatestItem, buildLatestItems, buildLatestQuery, searchPageUrl } from '../../utils/content-view'
import { toErrorMessage } from '../../utils/error'
import {
  BannerCard,
  NavTab,
  RecommendCard,
  toBannerCards,
  toContentRecommendations,
  toLatestCards,
  toNavTabs,
  toTopicRecommendations
} from '../../utils/home-view'
import { topicListUrl } from '../../utils/topic-view'

interface CategoryPanel {
  categoryId: string
  name: string
  status: PageStatus
  message: string
  children: CategoryChildLink[]
  latest: LatestItem[]
  latestStatus: PageStatus
  latestError: string
  loaded: boolean
  loading: boolean
}

interface IndexPageData {
  status: PageStatus
  error: string
  banners: BannerCard[]
  tabs: NavTab[]
  activeTab: number
  panels: CategoryPanel[]
  recommendations: RecommendCard[]
  topics: RecommendCard[]
  latest: LatestItem[]
  topicListUrl: string
  loading: boolean
  tree: CategoryTreeVO[]
}

Page<IndexPageData, WechatMiniprogram.IAnyObject>({
  data: {
    status: 'loading',
    error: '',
    banners: [],
    tabs: toNavTabs([]),
    activeTab: 0,
    panels: [],
    recommendations: [],
    topics: [],
    latest: [],
    topicListUrl: topicListUrl(),
    loading: false,
    tree: []
  },

  onLoad() {
    this.reload()
  },

  onPullDownRefresh() {
    const tab = this.data.tabs[this.data.activeTab]
    const task = !tab || tab.kind === 'home'
      ? this.reload()
      : this.loadCategoryPanel(this.data.activeTab, true)
    task.finally(() => {
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

  onTabTap(event: WechatMiniprogram.TouchEvent) {
    const index = Number(event.currentTarget.dataset.index)
    if (!Number.isInteger(index) || index < 0 || index === this.data.activeTab) {
      return
    }
    this.setData({ activeTab: index })
    this.ensureCategoryPanel(index)
  },

  onSwiperChange(event: WechatMiniprogram.SwiperChange) {
    const index = Number(event.detail.current)
    if (!Number.isInteger(index) || index < 0) {
      return
    }
    this.setData({ activeTab: index })
    this.ensureCategoryPanel(index)
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

  ensureCategoryPanel(tabIndex: number) {
    const tab = this.data.tabs[tabIndex]
    if (!tab || tab.kind !== 'category') {
      return
    }
    const panel = this.data.panels[tabIndex - 1]
    if (!panel || panel.loaded || panel.loading) {
      return
    }
    void this.loadCategoryPanel(tabIndex, false)
  },

  loadCategoryPanel(tabIndex: number, force: boolean) {
    const tab = this.data.tabs[tabIndex]
    if (!tab || tab.kind !== 'category') {
      return Promise.resolve()
    }
    const panelIndex = tabIndex - 1
    const panel = this.data.panels[panelIndex]
    if (!panel) {
      return Promise.resolve()
    }
    if (panel.loading) {
      return Promise.resolve()
    }
    if (panel.loaded && !force) {
      return Promise.resolve()
    }

    const categoryId = tab.categoryId
    this.setData({
      [`panels[${panelIndex}].loading`]: true,
      [`panels[${panelIndex}].status`]: 'loading',
      [`panels[${panelIndex}].message`]: '',
      [`panels[${panelIndex}].latestStatus`]: 'loading',
      [`panels[${panelIndex}].latestError`]: ''
    })

    const treePromise = this.data.tree.length
      ? Promise.resolve(this.data.tree)
      : getCategoryTree().then((tree) => {
          this.setData({ tree })
          return tree
        })

    return treePromise
      .then((tree) => {
        const category = findCategory(tree, categoryId)
        if (!category) {
          this.setData({
            [`panels[${panelIndex}].status`]: 'error',
            [`panels[${panelIndex}].message`]: '分类不存在',
            [`panels[${panelIndex}].children`]: [],
            [`panels[${panelIndex}].latest`]: [],
            [`panels[${panelIndex}].latestStatus`]: 'empty',
            [`panels[${panelIndex}].loaded`]: true,
            [`panels[${panelIndex}].loading`]: false
          })
          return
        }
        const children = listChildren(category).map((item) => ({
          id: item.id,
          name: item.name,
          url: contentListUrl(item.id)
        }))
        this.setData({
          [`panels[${panelIndex}].status`]: children.length ? 'success' : 'empty',
          [`panels[${panelIndex}].message`]: children.length ? '' : '暂无子分类',
          [`panels[${panelIndex}].children`]: children
        })
        return getContentList(buildLatestQuery(categoryId)).then((page) => {
          const records = page.records || []
          this.setData({
            [`panels[${panelIndex}].latest`]: buildLatestItems(records, tree),
            [`panels[${panelIndex}].latestStatus`]: records.length === 0 ? 'empty' : 'success',
            [`panels[${panelIndex}].loaded`]: true,
            [`panels[${panelIndex}].loading`]: false
          })
        })
      })
      .catch((error: unknown) => {
        this.setData({
          [`panels[${panelIndex}].status`]: 'error',
          [`panels[${panelIndex}].message`]: toErrorMessage(error, '分类加载失败，请稍后重试'),
          [`panels[${panelIndex}].latestStatus`]: 'error',
          [`panels[${panelIndex}].latestError`]: toErrorMessage(error, '内容加载失败，请稍后重试'),
          [`panels[${panelIndex}].loaded`]: false,
          [`panels[${panelIndex}].loading`]: false
        })
      })
  },

  retryCategoryPanel(event: WechatMiniprogram.TouchEvent) {
    const index = Number(event.currentTarget.dataset.index)
    void this.loadCategoryPanel(index, true)
  },

  reload() {
    if (this.data.loading) {
      return Promise.resolve()
    }
    this.data.loading = true
    this.setData({ loading: true, status: 'loading', error: '' })
    return getHome()
      .then((page) => {
        const tabs = toNavTabs(page.categories || [])
        const panels = tabs
          .filter((item) => item.kind === 'category')
          .map((item) => emptyPanel(item.categoryId, item.name))
        const activeTab = Math.min(this.data.activeTab, Math.max(tabs.length - 1, 0))
        this.setData({
          banners: toBannerCards(page.banners || []),
          tabs,
          panels,
          activeTab,
          recommendations: toContentRecommendations(page.recommendations || []),
          topics: toTopicRecommendations(page.topics || []),
          latest: toLatestCards(page.latestContents || []),
          status: 'success',
          error: '',
          tree: []
        })
        if (activeTab > 0) {
          this.ensureCategoryPanel(activeTab)
        }
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

function emptyPanel(categoryId: string, name: string): CategoryPanel {
  return {
    categoryId,
    name,
    status: 'loading',
    message: '',
    children: [],
    latest: [],
    latestStatus: 'loading',
    latestError: '',
    loaded: false,
    loading: false
  }
}

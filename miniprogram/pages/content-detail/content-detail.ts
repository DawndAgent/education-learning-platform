import { getArticleDetail } from '../../services/article'
import { getCategoryDetail } from '../../services/category'
import { getContentDetail, recordContentView } from '../../services/content'
import { getDocumentDetail } from '../../services/document'
import { getQuestionDetail } from '../../services/question'
import { getVideoDetail } from '../../services/video'
import { getWeeklyDetail } from '../../services/weekly'
import { DetailView, blankDetail, isHttpUrl, loadContentDetail } from '../../utils/content-detail'
import { contentDetailUrl, resolveContentIdFromQuery } from '../../utils/content-view'

interface ContentDetailPageData extends DetailView {
  contentId: string
  generation: number
  viewed: boolean
  answerExpanded: boolean
  analysisExpanded: boolean
}

Page<ContentDetailPageData, WechatMiniprogram.IAnyObject>({
  data: {
    ...blankDetail(),
    contentId: '',
    generation: 0,
    viewed: false,
    answerExpanded: false,
    analysisExpanded: false
  },

  onLoad(query) {
    const contentId = resolveContentIdFromQuery({
      id: query.id,
      scene: query.scene
    })
    this.setData({ contentId, viewed: false, answerExpanded: false, analysisExpanded: false })
    this.data.contentId = contentId
    this.data.viewed = false
    this.loadDetail()
  },

  onShareAppMessage() {
    const title = this.data.title || 'XX教育学习中心'
    const path = contentDetailUrl(this.data.contentId)
    const imageUrl = this.data.coverUrl || undefined
    return imageUrl ? { title, path, imageUrl } : { title, path }
  },

  loadDetail() {
    const generation = this.data.generation + 1
    this.setData({
      ...blankDetail(),
      generation,
      contentId: this.data.contentId,
      viewed: false,
      answerExpanded: false,
      analysisExpanded: false
    })
    this.data.generation = generation
    this.data.viewed = false
    const contentId = this.data.contentId
    loadContentDetail(contentId, {
      getContentDetail,
      getArticleDetail,
      getVideoDetail,
      getQuestionDetail,
      getWeeklyDetail,
      getDocumentDetail,
      getCategoryDetail
    })
      .then((view) => {
        if (this.data.generation !== generation) {
          return
        }
        this.setData({ ...view, answerExpanded: false, analysisExpanded: false })
        if (view.title) {
          wx.setNavigationBarTitle({ title: view.title })
        }
        if (view.status === 'success' && contentId) {
          this.recordViewOnce(contentId)
        }
      })
      .catch(() => {
        if (this.data.generation !== generation) {
          return
        }
        this.setData({
          status: 'error',
          message: '内容加载失败，请稍后重试',
          canRetry: true
        })
      })
  },

  recordViewOnce(contentId: string) {
    if (this.data.viewed) {
      return
    }
    this.data.viewed = true
    this.setData({ viewed: true })
    recordContentView(contentId).catch(() => {
      // 浏览统计失败不影响阅读
    })
  },

  goBack() {
    wx.navigateBack({
      fail() {
        wx.redirectTo({ url: '/pages/index/index' })
      }
    })
  },

  previewQr() {
    const url = this.data.qrCodeUrl
    if (!url) {
      return
    }
    wx.previewImage({
      current: url,
      urls: [url]
    })
  },

  previewQuestionImage() {
    const url = this.data.questionImageUrl
    if (!url) {
      return
    }
    wx.previewImage({
      current: url,
      urls: [url]
    })
  },

  previewAnswerImage() {
    const url = this.data.answerImageUrl
    if (!url) {
      return
    }
    wx.previewImage({
      current: url,
      urls: [url]
    })
  },

  previewAnalysisImage() {
    const url = this.data.analysisImageUrl
    if (!url) {
      return
    }
    wx.previewImage({
      current: url,
      urls: [url]
    })
  },

  toggleAnswer() {
    this.setData({ answerExpanded: !this.data.answerExpanded })
  },

  toggleAnalysis() {
    this.setData({ analysisExpanded: !this.data.analysisExpanded })
  },

  previewDocument() {
    const url = (this.data.previewUrl || this.data.fileUrl || '').trim()
    if (!url) {
      wx.showToast({ title: '暂无预览地址', icon: 'none' })
      return
    }
    if (!isHttpUrl(url)) {
      wx.showToast({ title: '预览地址无效', icon: 'none' })
      return
    }
    if (isImageUrl(url, this.data.fileType)) {
      wx.previewImage({ current: url, urls: [url] })
      return
    }
    wx.downloadFile({
      url,
      success(res) {
        if (res.statusCode !== 200 || !res.tempFilePath) {
          copyLink(url, '预览链接已复制')
          return
        }
        wx.openDocument({
          filePath: res.tempFilePath,
          showMenu: true,
          fail() {
            copyLink(url, '预览链接已复制')
          }
        })
      },
      fail() {
        copyLink(url, '预览链接已复制')
      }
    })
  },

  downloadDocument() {
    const url = (this.data.downloadUrl || this.data.fileUrl || this.data.previewUrl || '').trim()
    if (!url) {
      wx.showToast({ title: '暂无下载地址', icon: 'none' })
      return
    }
    copyLink(url, '下载链接已复制')
  }
})

function copyLink(url: string, title: string) {
  wx.setClipboardData({
    data: url,
    success() {
      wx.showToast({ title, icon: 'none' })
    },
    fail() {
      wx.showToast({ title: '复制失败，请稍后重试', icon: 'none' })
    }
  })
}

function isImageUrl(url: string, fileType: string): boolean {
  const type = (fileType || '').toLowerCase()
  if (/^(jpe?g|png|gif|webp|bmp)$/.test(type)) {
    return true
  }
  return /\.(jpe?g|png|gif|webp|bmp)(\?|#|$)/i.test(url)
}

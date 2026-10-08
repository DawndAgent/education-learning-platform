import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import test from 'node:test'
import { createRequire } from 'node:module'
import ts from 'typescript'

const require = createRequire(import.meta.url)

Module._extensions['.ts'] = (module, filename) => {
  const source = fs.readFileSync(filename, 'utf8')
  const compiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022
    },
    fileName: filename
  }).outputText.replace(/require\((["'])(\.[^"']+)\1\)/g, (_match, quote, spec) => {
    const resolved = path.resolve(path.dirname(filename), spec)
    const tsFile = fs.existsSync(`${resolved}.ts`) ? `${resolved}.ts` : resolved
    return `require(${quote}${tsFile.split('\\').join('/')}${quote})`
  })
  module._compile(compiled, filename)
}

const detail = require('../miniprogram/utils/content-detail.ts')
const errors = require('../miniprogram/utils/error.ts')
const contentView = require('../miniprogram/utils/content-view.ts')
const contentList = require('../miniprogram/utils/content-list.ts')

function content(id, contentType, coverUrl = 'content-cover') {
  return {
    id,
    title: contentType === 'VIDEO' ? 'PET 听力精讲 01' : 'PET 高频词汇 1000 词',
    contentType,
    categoryId: '11',
    coverUrl,
    summary: 'PET 听力第一课',
    status: 'PUBLISHED',
    sort: 1,
    viewCount: '0',
    favoriteCount: '0',
    publishTime: '2026-09-24T08:00:00'
  }
}

function article(id) {
  return {
    contentId: id,
    title: 'PET 高频词汇 1000 词',
    categoryId: '11',
    coverUrl: 'article-cover',
    summary: null,
    status: 'PUBLISHED',
    sort: 1,
    publishTime: '2026-09-24T08:00:00',
    body: '<p>PET 是……</p><script>alert(1)</script><img src="photo.jpg" onerror="bad">',
    author: 'XX老师',
    source: '教研组'
  }
}

function video(id, sourceType, qrCodeUrl, duration, coverUrl = null, videoUrl = 'https://v.qq.com/x/page/example.html') {
  return {
    contentId: id,
    title: 'PET 听力精讲 01',
    categoryId: '11',
    coverUrl,
    summary: 'PET 听力第一课',
    status: 'PUBLISHED',
    sort: 1,
    publishTime: '2026-09-23T08:00:00',
    sourceType,
    videoUrl,
    qrCodeUrl,
    duration
  }
}

function services(handlers) {
  const calls = []
  const rejectMissing = (name) => () => Promise.reject(new Error(`should not request ${name}`))
  return {
    calls,
    getContentDetail(id) {
      calls.push(['content', id])
      return handlers.content()
    },
    getArticleDetail(id) {
      calls.push(['article', id])
      return handlers.article()
    },
    getVideoDetail(id) {
      calls.push(['video', id])
      return handlers.video()
    },
    getQuestionDetail(id) {
      calls.push(['question', id])
      return (handlers.question || rejectMissing('question'))()
    },
    getWeeklyDetail(id) {
      calls.push(['weekly', id])
      return (handlers.weekly || rejectMissing('weekly'))()
    },
    getDocumentDetail(id) {
      calls.push(['document', id])
      return (handlers.document || rejectMissing('document'))()
    },
    getCategoryDetail(id) {
      calls.push(['category', id])
      return (handlers.category || (() => Promise.resolve({ id, name: '' })))()
    }
  }
}

function rejectNotFound(message) {
  return Promise.reject(new errors.ApiError('404', message, 404))
}

test('id 缺失', async () => {
  const api = services({
    content: () => Promise.reject(new Error('should not request')),
    article: () => Promise.reject(new Error('should not request')),
    video: () => Promise.reject(new Error('should not request'))
  })
  const view = await detail.loadContentDetail('  ', api)
  assert.equal(view.status, 'error')
  assert.equal(view.message, '内容参数缺失')
  assert.equal(view.canRetry, false)
  assert.deepEqual(api.calls, [])
})

test('id 非法', async () => {
  const api = services({
    content: () => Promise.reject(new Error('should not request')),
    article: () => Promise.reject(new Error('should not request')),
    video: () => Promise.reject(new Error('should not request'))
  })
  const view = await detail.loadContentDetail('abc', api)
  assert.equal(view.message, '内容参数无效')
  assert.deepEqual(api.calls, [])
})

test('内容不存在', async () => {
  const api = services({
    content: () => rejectNotFound('内容不存在'),
    article: () => Promise.reject(new Error('should not request article')),
    video: () => Promise.reject(new Error('should not request video'))
  })
  const view = await detail.loadContentDetail('10001', api)
  assert.equal(view.status, 'error')
  assert.equal(view.message, '内容不存在或已下线')
  assert.equal(view.canRetry, true)
  assert.deepEqual(api.calls, [['content', '10001']])
})

test('内容加载失败', async () => {
  const api = services({
    content: () => Promise.reject(new errors.ApiError('500', 'java.sql.SQLException', 500)),
    article: () => Promise.reject(new Error('should not request article')),
    video: () => Promise.reject(new Error('should not request video'))
  })
  const view = await detail.loadContentDetail('10001', api)
  assert.equal(view.message, '内容加载失败，请稍后重试')
  assert.equal(view.canRetry, true)
  assert.deepEqual(api.calls, [['content', '10001']])
})

test('ARTICLE 正常加载', async () => {
  const api = services({
    content: () => Promise.resolve(content('10001', 'ARTICLE')),
    article: () => Promise.resolve(article('10001')),
    video: () => Promise.reject(new Error('should not request video'))
  })
  const view = await detail.loadContentDetail('10001', api)
  assert.equal(view.status, 'success')
  assert.equal(view.kind, 'article')
  assert.equal(view.title, 'PET 高频词汇 1000 词')
  assert.equal(view.author, 'XX老师')
  assert.equal(view.source, '教研组')
  assert.equal(view.dateText, '2026-09-24')
  assert.equal(view.coverUrl, 'article-cover')
  assert.deepEqual(api.calls, [['content', '10001'], ['article', '10001']])
})

test('本地上传封面和正文图会拼成可访问地址', async () => {
  const api = services({
    content: () => Promise.resolve(content('10001', 'ARTICLE', '/uploads/images/covers/a.png')),
    article: () => Promise.resolve({
      ...article('10001'),
      coverUrl: '/uploads/images/covers/a.png',
      body: '<p>正文</p><img src="/uploads/images/articles/b.png">'
    }),
    video: () => Promise.reject(new Error('should not request video'))
  })
  const view = await detail.loadContentDetail('10001', api)
  assert.equal(view.coverUrl, 'http://127.0.0.1:8080/uploads/images/covers/a.png')
  assert.match(view.bodyHtml, /http:\/\/127\.0\.0\.1:8080\/uploads\/images\/articles\/b\.png/)
})

test('Article API 404', async () => {
  const api = services({
    content: () => Promise.resolve(content('10001', 'ARTICLE')),
    article: () => rejectNotFound('文章不存在'),
    video: () => Promise.reject(new Error('should not request video'))
  })
  const view = await detail.loadContentDetail('10001', api)
  assert.equal(view.message, '内容不存在或已下线')
  assert.deepEqual(api.calls.map((item) => item[0]), ['content', 'article'])
})

test('Article API 失败', async () => {
  const api = services({
    content: () => Promise.resolve(content('10001', 'ARTICLE')),
    article: () => Promise.reject(new errors.ApiError('500', 'NullPointerException', 500)),
    video: () => Promise.reject(new Error('should not request video'))
  })
  const view = await detail.loadContentDetail('10001', api)
  assert.equal(view.message, '内容加载失败，请稍后重试')
  assert.equal(view.kind, '')
  assert.deepEqual(api.calls.map((item) => item[0]), ['content', 'article'])
})

test('HTML body 展示', async () => {
  const html = detail.prepareArticleHtml('<p>PET 是……</p><script>alert(1)</script><img src="photo.jpg" onerror="bad">')
  assert.match(html, /PET 是/)
  assert.equal(html.includes('<script'), false)
  assert.equal(html.includes('onerror'), false)
  assert.match(html, /max-width:100%/)
  const sized = detail.prepareArticleHtml('<img src="/uploads/q.png" style="width:180px;height:auto;">')
  assert.match(sized, /width:180px/)
  assert.match(sized, /max-width:100%/)
  const moved = detail.prepareArticleHtml(
    '<img src="/uploads/q.png" style="width:180px;margin-left:36px;margin-top:12px;">'
  )
  assert.match(moved, /margin-left:36px/)
  assert.match(moved, /margin-top:12px/)
  assert.match(moved, /width:180px/)
  const api = services({
    content: () => Promise.resolve(content('10001', 'ARTICLE')),
    article: () => Promise.resolve(article('10001')),
    video: () => Promise.reject(new Error('should not request video'))
  })
  const view = await detail.loadContentDetail('10001', api)
  assert.match(view.bodyHtml, /PET 是/)
  assert.equal(view.bodyHtml.includes('<script'), false)
})

test('VIDEO 正常加载', async () => {
  const api = services({
    content: () => Promise.resolve(content('20002', 'VIDEO', 'content-cover')),
    article: () => Promise.reject(new Error('should not request article')),
    video: () => Promise.resolve(video('20002', 'WECHAT_CHANNEL', null, 630, null))
  })
  const view = await detail.loadContentDetail('20002', api)
  assert.equal(view.kind, 'video')
  assert.equal(view.title, 'PET 听力精讲 01')
  assert.equal(view.coverUrl, 'content-cover')
  assert.equal(view.summary, 'PET 听力第一课')
  assert.equal(view.dateText, '2026-09-23')
  assert.deepEqual(api.calls.map((item) => item[0]), ['content', 'video'])
})

test('Video API 404', async () => {
  const api = services({
    content: () => Promise.resolve(content('20002', 'VIDEO')),
    article: () => Promise.reject(new Error('should not request article')),
    video: () => rejectNotFound('视频不存在')
  })
  const view = await detail.loadContentDetail('20002', api)
  assert.equal(view.message, '内容不存在或已下线')
  assert.deepEqual(api.calls.map((item) => item[0]), ['content', 'video'])
})

test('Video API 失败', async () => {
  const api = services({
    content: () => Promise.resolve(content('20002', 'VIDEO')),
    article: () => Promise.reject(new Error('should not request article')),
    video: () => Promise.reject(new errors.ApiError('NETWORK', 'request:fail'))
  })
  const view = await detail.loadContentDetail('20002', api)
  assert.equal(view.message, '网络异常，请稍后重试')
  assert.deepEqual(api.calls.map((item) => item[0]), ['content', 'video'])
})

test('WECHAT_CHANNEL', () => {
  assert.equal(detail.sourceLabel('WECHAT_CHANNEL'), '微信视频号观看')
})

test('TENCENT_VIDEO', () => {
  assert.equal(detail.sourceLabel('TENCENT_VIDEO'), '腾讯视频观看')
  assert.equal(detail.sourceLabel('LOCAL'), '平台内播放')
  assert.equal(detail.sourceLabel('OWN_STORAGE'), '暂不支持的播放来源')
})

test('本地上传视频可在小程序内播放', async () => {
  const api = services({
    content: () => Promise.resolve(content('20002', 'VIDEO', 'content-cover')),
    article: () => Promise.reject(new Error('should not request article')),
    video: () => Promise.resolve(video('20002', 'LOCAL', null, 12, null, '/uploads/videos/local/a.mp4'))
  })
  const view = await detail.loadContentDetail('20002', api)
  assert.equal(view.playUrl, 'http://127.0.0.1:8080/uploads/videos/local/a.mp4')
  assert.equal(view.watchHint, '')
  assert.equal(view.sourceLabel, '平台内播放')
})

test('二维码为空', async () => {
  const api = services({
    content: () => Promise.resolve(content('20002', 'VIDEO')),
    article: () => Promise.reject(new Error('should not request article')),
    video: () => Promise.resolve(video('20002', 'TENCENT_VIDEO', null, 630))
  })
  const view = await detail.loadContentDetail('20002', api)
  assert.equal(view.qrCodeUrl, '')
  assert.equal(view.watchHint, '请扫码观看完整视频')
})

test('二维码存在', async () => {
  const api = services({
    content: () => Promise.resolve(content('20002', 'VIDEO')),
    article: () => Promise.reject(new Error('should not request article')),
    video: () => Promise.resolve(video('20002', 'WECHAT_CHANNEL', 'qr-code', 630, 'video-cover'))
  })
  const view = await detail.loadContentDetail('20002', api)
  assert.equal(view.qrCodeUrl, 'qr-code')
  assert.equal(view.watchHint, '扫码观看完整视频')
  assert.equal(view.coverUrl, 'video-cover')
  assert.equal(view.sourceLabel, '微信视频号观看')
})

test('duration 格式化', () => {
  assert.equal(detail.formatDuration(630), '10:30')
  assert.equal(detail.formatDuration(59), '00:59')
  assert.equal(detail.formatDuration(3661), '01:01:01')
  assert.equal(detail.formatDuration(null), '')
})

test('不支持的 contentType', async () => {
  const api = services({
    content: () => Promise.resolve(content('30003', 'TOPIC')),
    article: () => Promise.reject(new Error('should not request article')),
    video: () => Promise.reject(new Error('should not request video'))
  })
  const view = await detail.loadContentDetail('30003', api)
  assert.equal(view.status, 'unsupported')
  assert.equal(view.message, '该内容类型暂不支持')
  assert.deepEqual(api.calls, [['content', '30003']])
})

test('从列表进入详情', () => {
  const record = {
    id: '10001',
    title: 'PET 高频词汇 1000 词',
    contentType: 'ARTICLE',
    categoryId: '11',
    coverUrl: null,
    summary: null,
    status: 'PUBLISHED',
    sort: 1,
    viewCount: '0',
    favoriteCount: '0',
    publishTime: '2026-09-24T08:00:00'
  }
  const card = contentList.toContentCard(record)
  assert.equal(card.url, contentView.contentDetailUrl('10001'))
  assert.equal(card.url, '/pages/content-detail/content-detail?id=10001')
})

test('小程序码 scene 解析为内容 ID', () => {
  assert.equal(contentView.resolveContentIdFromQuery({ id: '10001' }), '10001')
  assert.equal(contentView.resolveContentIdFromQuery({ scene: '10001' }), '10001')
  assert.equal(contentView.resolveContentIdFromQuery({ scene: encodeURIComponent('10001') }), '10001')
  assert.equal(contentView.resolveContentIdFromQuery({ scene: 'v=10001' }), '10001')
  assert.equal(contentView.resolveContentIdFromQuery({ id: '9', scene: '10001' }), '9')
  assert.equal(contentView.resolveContentIdFromQuery({}), '')
  const detailSource = fs.readFileSync(path.resolve('miniprogram/pages/content-detail/content-detail.ts'), 'utf8')
  assert.match(detailSource, /resolveContentIdFromQuery/)
})

test('返回列表', () => {
  const root = path.resolve('miniprogram/pages')
  const listSource = fs.readFileSync(path.join(root, 'content-list/content-list.ts'), 'utf8')
  const detailSource = fs.readFileSync(path.join(root, 'content-detail/content-detail.ts'), 'utf8')
  assert.equal(listSource.includes('onShow'), false)
  assert.match(detailSource, /navigateBack/)
  assert.match(detailSource, /onShareAppMessage/)
  assert.match(detailSource, /recordContentView/)
  assert.equal(detailSource.includes('getContentList'), false)
  assert.equal(detailSource.includes('wx.request'), false)
})

test('首页与搜索入口', () => {
  const appJson = JSON.parse(fs.readFileSync(path.resolve('miniprogram/app.json'), 'utf8'))
  assert.ok(appJson.pages.includes('pages/search/search'))
  const indexSource = fs.readFileSync(path.resolve('miniprogram/pages/index/index.ts'), 'utf8')
  assert.match(indexSource, /getHome/)
  assert.match(indexSource, /searchPageUrl|goSearch/)
  assert.match(contentView.buildLatestQuery().sort || '', /publishTime/)
  assert.equal(contentView.buildSearchQuery('KET', 2).keyword, 'KET')
  assert.equal(contentView.buildSearchQuery('KET', 2).pageNum, 2)
  assert.equal(contentView.searchPageUrl('KET'), '/pages/search/search?keyword=KET')
})

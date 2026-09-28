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

const article = require('../src/utils/article-form.ts')
const video = require('../src/utils/video-form.ts')
const articlePage = fs.readFileSync(path.resolve('src/views/articles/editor.vue'), 'utf8')
const videoPage = fs.readFileSync(path.resolve('src/views/videos/editor.vue'), 'utf8')
const contentPage = fs.readFileSync(path.resolve('src/views/contents/index.vue'), 'utf8')
const leaveGuard = fs.readFileSync(path.resolve('src/composables/useUnsavedLeave.ts'), 'utf8')

const leaves = ['11']
const articleForm = {
  title: 'KET阅读',
  categoryId: '11',
  coverUrl: '',
  summary: '',
  sort: 1,
  body: '<p>第一段</p>',
  author: '老师',
  source: '教材'
}

test('文章正文不能为空，保存时保留 HTML', () => {
  assert.equal(article.validateArticleForm(articleForm, leaves), null)
  assert.equal(article.validateArticleForm({ ...articleForm, body: '<p><br></p>' }, leaves), '正文不能为空')
  assert.equal(article.validateArticleForm({ ...articleForm, categoryId: '1' }, leaves), '内容只能选择二级分类')
  assert.equal(article.toArticlePayload(articleForm).body, '<p>第一段</p>')
})

test('视频草稿可以暂缺地址，发布时必须是合法 URL', () => {
  const form = {
    title: '听力课',
    categoryId: '11',
    coverUrl: '',
    summary: '',
    sort: 1,
    sourceType: 'WECHAT_CHANNEL',
    videoUrl: '',
    qrCodeUrl: '',
    duration: 65
  }
  assert.equal(video.validateVideoForm(form, leaves), null)
  assert.equal(video.validateVideoPublish(form, leaves), '视频地址不能为空')
  assert.equal(video.validateVideoPublish({ ...form, videoUrl: 'https://channels.weixin.qq.com/example' }, leaves), '二维码地址不能为空')
  assert.equal(video.validateVideoForm({ ...form, videoUrl: 'javascript:alert(1)' }, leaves), '视频地址不合法')
  assert.equal(video.validateVideoForm({ ...form, qrCodeUrl: 'not-a-url' }, leaves), '二维码地址不合法')
  assert.equal(video.validateVideoForm({ ...form, qrCodeUrl: '/uploads/qr.png' }, leaves), null)
  assert.equal(video.validateVideoForm({ ...form, sourceType: '' }, leaves), '请选择视频来源')
  assert.equal(video.sourceTypeLabel('TENCENT_VIDEO'), '腾讯视频')
  assert.equal(video.sourceTypeLabel('WECHAT_CHANNEL'), '微信视频号')
  assert.equal(video.formatDuration(65), '01:05')
  assert.equal(video.formatDuration(3665), '61:05')
  assert.equal(video.canPreviewQr('/uploads/qr.png'), true)
  assert.equal(video.canPreviewQr('https://example.com/qr.png'), true)
  assert.equal(video.canPreviewQr('qr.png'), false)
  const ready = { ...form, videoUrl: 'https://channels.weixin.qq.com/example', qrCodeUrl: 'https://example.com/qr.png' }
  assert.equal(video.validateVideoPublish(ready, leaves), null)
  assert.equal(video.toVideoPayload(ready).qrCodeUrl, 'https://example.com/qr.png')
})

test('文章和视频编辑页覆盖预览、发布、离开确认', () => {
  assert.match(contentPage, /新增文章/)
  assert.match(contentPage, /新增视频/)
  assert.match(contentPage, /editorPath/)
  assert.match(articlePage, /articleApi\.updateArticle/)
  assert.match(articlePage, /articleApi\.createArticle/)
  assert.match(articlePage, /RichTextEditor/)
  assert.match(articlePage, /保存草稿/)
  assert.match(articlePage, /预览/)
  assert.match(articlePage, /publishConfirmText/)
  assert.match(articlePage, /useUnsavedLeave/)
  assert.match(videoPage, /useUnsavedLeave/)
  assert.match(leaveGuard, /当前内容尚未保存，确定离开吗？/)
  assert.equal(articlePage.includes("from 'axios'"), false)
  assert.match(videoPage, /videoApi\.updateVideo/)
  assert.match(videoPage, /videoApi\.createVideo/)
  assert.match(videoPage, /二维码加载失败/)
  assert.match(videoPage, /查看二维码/)
  assert.match(videoPage, /保存草稿/)
  assert.match(videoPage, /预览/)
  assert.equal(videoPage.includes("from 'axios'"), false)
  assert.equal(videoPage.includes('type="file"'), false)
  assert.match(article.previewHtml('<p>正文</p><script>alert(1)</script><img src=x onerror=alert(1)>'), /正文/)
  assert.equal(article.previewHtml('<p>正文</p><script>alert(1)</script>').includes('script'), false)
})

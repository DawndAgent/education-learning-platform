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

const fileUpload = require('../src/utils/file-upload.ts')
const media = require('../src/utils/media-url.ts')
const video = require('../src/utils/video-form.ts')
const fileApiSource = fs.readFileSync(path.resolve('src/api/file.ts'), 'utf8')
const imageUpload = fs.readFileSync(path.resolve('src/components/ImageUpload.vue'), 'utf8')
const editor = fs.readFileSync(path.resolve('src/components/RichTextEditor.vue'), 'utf8')
const articlePage = fs.readFileSync(path.resolve('src/views/articles/editor.vue'), 'utf8')
const videoPage = fs.readFileSync(path.resolve('src/views/videos/editor.vue'), 'utf8')
const videoFileUpload = fs.readFileSync(path.resolve('src/components/VideoFileUpload.vue'), 'utf8')

test('前端图片上传校验', () => {
  assert.equal(fileUpload.validateImageFile({ size: 0, type: 'image/png' }), '文件不能为空')
  assert.equal(fileUpload.validateImageFile({ size: 11 * 1024 * 1024, type: 'image/png' }), '文件大小不能超过10MB')
  assert.equal(fileUpload.validateImageFile({ size: 10, type: 'text/plain' }), '仅支持 JPEG、PNG、WEBP、GIF 图片')
  assert.equal(fileUpload.validateImageFile({ size: 10, type: 'image/png' }), null)
  assert.equal(media.isMediaUrl('/uploads/a.png'), true)
  assert.equal(media.isMediaUrl('https://example.com/a.png'), true)
  assert.equal(media.isMediaUrl('../x.png'), false)
  assert.equal(video.canPreviewQr('/uploads/qr.png'), true)
  assert.equal(fileUpload.validateVideoFile({ name: 'a.mp4', size: 10, type: 'video/mp4' }), null)
  assert.equal(fileUpload.validateVideoFile({ name: 'a.mov', size: 10, type: 'video/quicktime' }), '仅支持 MP4 视频')
  assert.equal(fileUpload.validateVideoFile({ name: 'a.mp4', size: 51 * 1024 * 1024, type: 'video/mp4' }), '视频大小不能超过50MB')
  assert.equal(fileUpload.validatePdfFile({ name: 'a.pdf', size: 10 }), null)
  assert.equal(fileUpload.validatePdfFile({ name: 'a.docx', size: 10 }), '题目导入仅支持 PDF 文件')
  assert.equal(fileUpload.validatePdfFile({ name: 'a.pdf', size: 21 * 1024 * 1024 }), '文件大小不能超过20MB')
  assert.equal(video.validateVideoPublish({
    title: '听力课',
    categoryId: '11',
    coverUrl: '/uploads/cover.png',
    summary: '',
    sort: 1,
    sourceType: 'WECHAT_CHANNEL',
    videoUrl: 'https://channels.weixin.qq.com/example',
    qrCodeUrl: '/uploads/qr.png',
    duration: 30
  }, ['11']), null)
})

test('上传组件和编辑器接入上传接口', () => {
  assert.match(fileApiSource, /\/admin\/api\/files\/upload/)
  assert.match(fileApiSource, /onUploadProgress/)
  assert.match(imageUpload, /fileApi\.uploadImage/)
  assert.match(imageUpload, /onProgress/)
  assert.match(imageUpload, /删除/)
  assert.match(editor, /customUpload/)
  assert.match(editor, /scene: 'ARTICLE'/)
  assert.match(editor, /fileApi\.uploadImage/)
  assert.match(articlePage, /ImageUpload/)
  assert.match(articlePage, /scene="COVER"/)
  assert.match(fileApiSource, /uploadVideo/)
  assert.match(fileApiSource, /VIDEO_FILE/)
  assert.match(videoPage, /scene="VIDEO"/)
  assert.match(videoPage, /scene="QRCODE"/)
  assert.match(videoFileUpload, /fileApi\.uploadVideo/)
  assert.match(videoFileUpload, /accept="video\/mp4,.mp4"/)
  assert.equal(imageUpload.includes("from 'axios'"), false)
})

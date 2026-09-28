import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

test('专题页面与 service 已注册', () => {
  const appJson = JSON.parse(fs.readFileSync(path.resolve('miniprogram/app.json'), 'utf8'))
  assert.ok(appJson.pages.includes('pages/topic-list/topic-list'))
  assert.ok(appJson.pages.includes('pages/topic-detail/topic-detail'))
  const service = fs.readFileSync(path.resolve('miniprogram/services/topic.ts'), 'utf8')
  assert.match(service, /\/api\/topics/)
  assert.match(service, /\/api\/topics\/featured/)
  assert.equal(service.includes('wx.request'), false)
  const index = fs.readFileSync(path.resolve('miniprogram/pages/index/index.ts'), 'utf8')
  const homeView = fs.readFileSync(path.resolve('miniprogram/utils/home-view.ts'), 'utf8')
  assert.match(index, /getHome/)
  assert.match(homeView, /topicDetailUrl/)
  assert.match(index, /topicListUrl|goTopicList/)
  const detail = fs.readFileSync(path.resolve('miniprogram/pages/topic-detail/topic-detail.ts'), 'utf8')
  assert.match(detail, /getTopicDetail/)
  assert.match(detail, /contentDetailUrl/)
  assert.equal(detail.includes('wx.request'), false)
})

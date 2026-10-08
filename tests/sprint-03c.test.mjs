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

const list = require('../miniprogram/utils/content-list.ts')
const errors = require('../miniprogram/utils/error.ts')

function record(id, contentType, title) {
  return {
    id,
    title: title || `内容 ${id}`,
    contentType,
    categoryId: '11',
    coverUrl: contentType === 'VIDEO' ? null : `cover-${id}`,
    summary: contentType === 'ARTICLE' ? `${title || id} 摘要` : null,
    status: 'PUBLISHED',
    sort: 1,
    viewCount: '0',
    favoriteCount: '0',
    publishTime: '2026-09-24T08:00:00'
  }
}

function page(pageNum, total, records, pageSize = 10) {
  return { pageNum, pageSize, total, records }
}

function loadFirst(records, total = records.length) {
  const started = list.startFirst(list.createSession())
  return list.commitPage(started.session, started.generation, page(1, total, records), 'replace')
}

test('categoryId 缺失', () => {
  const result = list.resolveCategoryParam('  ')
  assert.equal(result.ok, false)
  assert.equal(result.message, '分类参数缺失')
  const session = list.invalidParamSession(result.message)
  assert.equal(session.status, 'error')
  assert.equal(session.canRetry, false)
  assert.equal(session.message, '分类参数缺失')
})

test('categoryId 非法', () => {
  const result = list.resolveCategoryParam('abc')
  assert.equal(result.ok, false)
  assert.equal(result.message, '分类参数无效')
})

test('正常加载第一页', () => {
  const article = record('101', 'ARTICLE', 'PET 高频词汇 1000 词')
  const video = record('102', 'VIDEO', 'PET 听力精讲01')
  const started = list.startFirst(list.createSession())
  const session = list.commitPage(started.session, started.generation, page(1, 24, [article, video]), 'replace')
  assert.equal(session.status, 'success')
  assert.equal(session.pageNum, 1)
  assert.equal(session.pageSize, 10)
  assert.equal(session.total, 24)
  assert.equal(session.hasMore, true)
  assert.equal(session.items[0].typeLabel, '文章')
  assert.equal(session.items[0].summary, 'PET 高频词汇 1000 词 摘要')
  assert.equal(session.items[0].dateText, '2026-09-24')
  assert.equal(session.items[1].typeLabel, '视频')
  assert.equal(session.items[1].coverUrl, '')
  assert.deepEqual(list.buildCategoryContentQuery('11', 1), {
    categoryId: '11',
    pageNum: 1,
    pageSize: 10
  })
})

test('空列表', () => {
  const session = loadFirst([])
  assert.equal(session.status, 'empty')
  assert.equal(session.message, '暂无内容')
  assert.equal(session.hasMore, false)
  assert.equal(session.items.length, 0)
})

test('请求失败', () => {
  const started = list.startFirst(list.createSession())
  const message = list.loadFailureMessage(new errors.ApiError('500', 'java.sql.SQLException', 500))
  const session = list.failLoad(started.session, started.generation, message)
  assert.equal(message, '内容加载失败，请稍后重试')
  assert.equal(session.status, 'error')
  assert.equal(session.message, '内容加载失败，请稍后重试')
  assert.equal(session.canRetry, true)
  assert.equal(session.items.length, 0)
})

test('第二页追加', () => {
  const first = Array.from({ length: 10 }, (_, index) => record(String(index + 1), 'ARTICLE'))
  const second = Array.from({ length: 10 }, (_, index) => record(String(index + 11), 'VIDEO'))
  let session = loadFirst(first, 24)
  const started = list.startMore(session)
  session = list.commitPage(started.session, started.generation, page(2, 24, second), 'append')
  assert.equal(session.pageNum, 2)
  assert.equal(session.items.length, 20)
  assert.deepEqual(session.items.map((item) => item.id), [...first, ...second].map((item) => item.id))
  assert.equal(session.hasMore, true)
})

test('第三页追加', () => {
  const first = Array.from({ length: 10 }, (_, index) => record(String(index + 1), 'ARTICLE'))
  const second = Array.from({ length: 10 }, (_, index) => record(String(index + 11), 'ARTICLE'))
  const third = [record('21', 'VIDEO'), record('22', 'ARTICLE'), record('23', 'VIDEO'), record('24', 'ARTICLE')]
  let session = loadFirst(first, 24)
  const page2 = list.startMore(session)
  session = list.commitPage(page2.session, page2.generation, page(2, 24, second), 'append')
  const page3 = list.startMore(session)
  session = list.commitPage(page3.session, page3.generation, page(3, 24, third), 'append')
  assert.equal(session.pageNum, 3)
  assert.equal(session.items.length, 24)
  assert.equal(session.items[20].id, '21')
  assert.equal(session.items[23].id, '24')
})

test('hasMore false', () => {
  const records = [record('1', 'ARTICLE'), record('2', 'VIDEO')]
  const session = loadFirst(records, 2)
  assert.equal(session.hasMore, false)
  assert.equal(session.total, 2)
  assert.equal(list.startMore(session), null)
})

test('重复触底不会重复请求', () => {
  const records = Array.from({ length: 10 }, (_, index) => record(String(index + 1), 'ARTICLE'))
  const session = loadFirst(records, 24)
  const requests = []
  let current = session
  for (let attempt = 0; attempt < 3; attempt += 1) {
    const nextPage = current.pageNum + 1
    const started = list.startMore(current)
    if (!started) {
      continue
    }
    requests.push(nextPage)
    current = started.session
  }
  assert.deepEqual(requests, [2])
  assert.equal(current.loadingMore, true)
  assert.equal(list.startMore(current), null)
})

test('下拉刷新重新从第一页加载', () => {
  const first = Array.from({ length: 10 }, (_, index) => record(String(index + 1), 'ARTICLE'))
  let session = loadFirst(first, 24)
  const more = list.startMore(session)
  session = list.commitPage(more.session, more.generation, page(2, 24, [record('11', 'VIDEO')]), 'append')
  assert.equal(session.pageNum, 2)
  const refreshed = list.startRefresh(session)
  assert.equal(refreshed.session.pageNum, 2)
  assert.equal(refreshed.session.refreshing, true)
  assert.equal(list.startMore(refreshed.session), null)
  const replaced = list.commitPage(
    refreshed.session,
    refreshed.generation,
    page(1, 3, [record('201', 'ARTICLE'), record('202', 'VIDEO')]),
    'replace'
  )
  assert.equal(replaced.pageNum, 1)
  assert.deepEqual(replaced.items.map((item) => item.id), ['201', '202'])
  assert.equal(replaced.refreshing, false)
})

test('刷新不会追加旧数据', () => {
  const session = loadFirst([record('1', 'ARTICLE'), record('2', 'VIDEO')], 2)
  const refreshed = list.startRefresh(session)
  const ignored = list.commitPage(refreshed.session, session.generation, page(2, 9, [record('9', 'ARTICLE')]), 'append')
  assert.deepEqual(ignored.items.map((item) => item.id), ['1', '2'])
  const replaced = list.commitPage(refreshed.session, refreshed.generation, page(1, 1, [record('8', 'VIDEO')]), 'replace')
  assert.deepEqual(replaced.items.map((item) => item.id), ['8'])
})

test('加载更多失败不会清空已有数据', () => {
  const session = loadFirst([record('1', 'ARTICLE'), record('2', 'VIDEO')], 12)
  const started = list.startMore(session)
  const failed = list.failMore(started.session, started.generation)
  assert.deepEqual(failed.items.map((item) => item.id), ['1', '2'])
  assert.equal(failed.loadingMore, false)
  assert.equal(failed.moreError, '加载更多失败，请重试')
  assert.equal(failed.hasMore, true)
  assert.equal(failed.status, 'success')
  assert.notEqual(list.startMore(failed), null)
})

test('点击内容进入详情', () => {
  const card = list.toContentCard(record('345', 'ARTICLE', 'PET 高频词汇 1000 词'))
  assert.equal(card.id, '345')
  assert.equal(card.url, '/pages/content-detail/content-detail?id=345')
  const uploaded = list.toContentCard({
    ...record('346', 'ARTICLE', '封面内容'),
    coverUrl: '/uploads/images/covers/a.png'
  })
  assert.equal(uploaded.coverUrl, 'http://127.0.0.1:8080/uploads/images/covers/a.png')
})

test('分类不存在', () => {
  assert.equal(list.loadFailureMessage(new errors.ApiError('404', '分类不存在', 404)), '分类不存在')
  assert.equal(list.resolveCategoryParam('11').ok, true)
})

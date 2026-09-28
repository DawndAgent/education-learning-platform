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

const store = new Map()
globalThis.wx = {
  getStorageSync(key) {
    return store.has(key) ? store.get(key) : ''
  },
  setStorageSync(key, value) {
    store.set(key, value)
  },
  removeStorageSync(key) {
    store.delete(key)
  }
}

const history = require('../miniprogram/utils/search-history.ts')
const contentView = require('../miniprogram/utils/content-view.ts')
const contentList = require('../miniprogram/utils/content-list.ts')

test('本地搜索历史最多 10 条且去重置顶', () => {
  store.clear()
  assert.deepEqual(history.readSearchHistory(), [])
  for (let i = 1; i <= 12; i += 1) {
    history.pushSearchHistory(`词${i}`)
  }
  const items = history.readSearchHistory()
  assert.equal(items.length, 10)
  assert.equal(items[0], '词12')
  assert.equal(items.includes('词1'), false)
  history.pushSearchHistory('词11')
  assert.equal(history.readSearchHistory()[0], '词11')
  history.clearSearchHistory()
  assert.deepEqual(history.readSearchHistory(), [])
})

test('搜索查询参数', () => {
  const query = contentView.buildSearchQuery('  KET  ', 3)
  assert.equal(query.keyword, 'KET')
  assert.equal(query.pageNum, 3)
  assert.equal(query.pageSize, 10)
  assert.equal(query.sort, 'publishTime')
})

test('上拉加载并发保护', () => {
  const session = {
    ...contentList.createSession(),
    status: 'success',
    hasMore: true,
    items: [{ id: '1', title: 'a', coverUrl: '', typeLabel: '文章', summary: '', dateText: '', url: '' }]
  }
  const first = contentList.startMore(session)
  assert.ok(first)
  const second = contentList.startMore(first.session)
  assert.equal(second, null)
})

test('搜索页复用 service 且无 wx.request', () => {
  const searchTs = fs.readFileSync(path.resolve('miniprogram/pages/search/search.ts'), 'utf8')
  assert.match(searchTs, /getContentList/)
  assert.match(searchTs, /buildSearchQuery/)
  assert.equal(searchTs.includes('wx.request'), false)
  const contentService = fs.readFileSync(path.resolve('miniprogram/services/content.ts'), 'utf8')
  assert.match(contentService, /\/api\/content\/\$\{encodeURIComponent\(id\)\}\/view/)
})

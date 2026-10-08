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

const categoryTree = require('../miniprogram/utils/category-tree.ts')
const contentView = require('../miniprogram/utils/content-view.ts')
const errors = require('../miniprogram/utils/error.ts')

function category(id, parentId, name, sort, children = []) {
  return {
    id,
    parentId,
    name,
    code: name,
    iconUrl: null,
    description: null,
    sort,
    status: 'ENABLED',
    children
  }
}

const tree = [
  category('30', '0', '初中自主学习', 3, [
    category('31', '30', '九年级｜资料&视频', 3),
    category('32', '30', '七年级｜资料&视频', 1)
  ]),
  category('10', '0', '剑桥英语', 1, [
    category('12', '10', '剑桥原版阅读', 2),
    category('11', '10', 'KET/PET备考资料', 1)
  ]),
  category('20', '0', '数学思维', 2, [
    category('21', '20', '每周一题', 1)
  ])
]

test('首页按 parentId 和 sort 展示一级分类，不依赖固定编号', () => {
  const entries = categoryTree.buildHomeEntries(tree)
  assert.deepEqual(entries.map((item) => item.name), ['剑桥英语', '数学思维', '初中自主学习'])
  assert.equal(entries[0].url, '/pages/category/category?id=10')
  assert.equal(entries[0].summary, 'KET/PET备考资料 / 剑桥原版阅读')
  assert.equal(entries.some((item) => item.id === '1'), false)
})

test('分类接口失败时不展示内部错误', () => {
  const error = new errors.ApiError('500', 'java.sql.SQLException: bad sql', 500)
  assert.equal(errors.toErrorMessage(error, '分类加载失败，请稍后重试'), '分类加载失败，请稍后重试')
})

test('内容接口失败时显示友好提示', () => {
  const error = new errors.ApiError('NETWORK', 'request:fail')
  assert.equal(errors.toErrorMessage(error, '内容加载失败，请稍后重试'), '网络异常，请稍后重试')
  const internal = new errors.ApiError('500', 'NullPointerException', 500)
  assert.equal(errors.toErrorMessage(internal), '内容加载失败，请稍后重试')
})

test('最新内容为空，且查询不带 categoryId', () => {
  const query = contentView.buildLatestQuery()
  assert.equal(query.pageNum, 1)
  assert.equal(query.pageSize, 10)
  assert.equal(query.sort, 'publishTime')
  assert.equal('categoryId' in query, false)
  assert.deepEqual(contentView.buildLatestItems([], tree), [])
})

test('点击一级分类和最新内容时使用接口返回的编号', () => {
  const [entry] = categoryTree.buildHomeEntries(tree)
  assert.equal(categoryTree.categoryPageUrl(entry.id), '/pages/category/category?id=10')
  const [item] = contentView.buildLatestItems([
    {
      id: '9001',
      title: 'PET高频词汇整理',
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
  ], tree)
  assert.equal(item.url, '/pages/content-detail/content-detail?id=9001')
  assert.equal(item.typeLabel, '文章')
  assert.equal(item.meta, '剑桥英语 · KET/PET备考资料')
  assert.equal(item.dateText, '2026-09-24')
  assert.equal(item.coverUrl, '')
})

test('分类页正常加载并按 sort 展示子分类', () => {
  const model = categoryTree.resolveCategoryPage({
    rawId: '10',
    detailName: '剑桥英语',
    tree,
    failure: null
  })
  assert.equal(model.status, 'success')
  assert.equal(model.name, '剑桥英语')
  assert.deepEqual(model.children.map((item) => item.name), ['KET/PET备考资料', '剑桥原版阅读'])
  assert.equal(model.children[0].url, '/pages/content-list/content-list?categoryId=11')
})

test('分类编号缺失、非法或不存在', () => {
  assert.equal(categoryTree.resolveCategoryPage({
    rawId: '',
    detailName: null,
    tree: null,
    failure: null
  }).message, '请从首页选择分类')

  assert.equal(categoryTree.resolveCategoryPage({
    rawId: 'abc',
    detailName: null,
    tree: null,
    failure: null
  }).message, '分类不存在')

  assert.equal(categoryTree.resolveCategoryPage({
    rawId: '99',
    detailName: null,
    tree: null,
    failure: { code: '404', httpStatus: 404, message: '分类不存在' }
  }).message, '分类不存在')
})

test('没有子分类时给出空状态', () => {
  const model = categoryTree.resolveCategoryPage({
    rawId: '40',
    detailName: '空分类',
    tree: [category('40', '0', '空分类', 1, [])],
    failure: null
  })
  assert.equal(model.status, 'empty')
  assert.equal(model.message, '暂无子分类')
  assert.deepEqual(model.children, [])
})

test('首页没有写死分类编号', () => {
  const page = fs.readFileSync(new URL('../miniprogram/pages/index/index.wxml', import.meta.url), 'utf8')
  assert.equal(page.includes('categoryId='), false)
  assert.equal(page.includes('id=1'), false)
})

test('首页底部导航支持滑动切换', () => {
  const homeView = require('../miniprogram/utils/home-view.ts')
  const tabs = homeView.toNavTabs([
    { id: '10', name: '剑桥英语', code: 'en', iconUrl: null },
    { id: '20', name: '数学思维', code: 'math', iconUrl: '/uploads/a.png' },
    { id: '30', name: '初中自主学习', code: 'mid', iconUrl: null }
  ])
  assert.equal(tabs[0].kind, 'home')
  assert.equal(tabs[0].shortName, '首页')
  assert.equal(tabs[1].shortName, '剑桥英语')
  assert.equal(tabs[3].shortName, '初中自主')
  assert.equal(tabs[2].iconUrl.endsWith('/uploads/a.png'), true)
  assert.match(tabs[2].iconUrl, /^https?:\/\//)
  const page = fs.readFileSync(new URL('../miniprogram/pages/index/index.wxml', import.meta.url), 'utf8')
  assert.match(page, /main-swiper/)
  assert.match(page, /onSwiperChange/)
  assert.match(page, /onTabTap/)
  const source = fs.readFileSync(new URL('../miniprogram/pages/index/index.ts', import.meta.url), 'utf8')
  assert.match(source, /onSwiperChange/)
  assert.match(source, /ensureCategoryPanel/)
})

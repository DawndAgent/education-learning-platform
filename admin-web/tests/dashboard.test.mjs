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

const access = require('../src/utils/access.ts')

function categoryPercent(count, total) {
  if (total <= 0) {
    return 0
  }
  return Math.round((count / total) * 100)
}

function editPath(row) {
  return row.contentType === 'VIDEO' ? `/videos/${row.id}` : `/articles/${row.id}`
}

function visibleShortcuts(permissions) {
  return {
    createArticle: permissions.includes('CONTENT_CREATE'),
    createVideo: permissions.includes('VIDEO_MANAGE'),
    contents: permissions.includes('CONTENT_VIEW'),
    categories: permissions.includes('CATEGORY_MANAGE'),
  }
}

test('Dashboard 菜单需要 DASHBOARD_VIEW', () => {
  assert.equal(access.canSeeMenu('/dashboard', []), false)
  assert.equal(access.canSeeMenu('/dashboard', ['DASHBOARD_VIEW']), true)
})

test('分类占比按 contentTotal 计算', () => {
  assert.equal(categoryPercent(50, 128), 39)
  assert.equal(categoryPercent(0, 0), 0)
})

test('最近发布跳转按内容类型', () => {
  assert.equal(editPath({ id: '100', contentType: 'ARTICLE' }), '/articles/100')
  assert.equal(editPath({ id: '200', contentType: 'VIDEO' }), '/videos/200')
})

test('快捷操作按权限显示', () => {
  assert.deepEqual(visibleShortcuts([]), {
    createArticle: false,
    createVideo: false,
    contents: false,
    categories: false,
  })
  assert.deepEqual(visibleShortcuts(['CONTENT_CREATE', 'CONTENT_VIEW', 'CATEGORY_MANAGE']), {
    createArticle: true,
    createVideo: false,
    contents: true,
    categories: true,
  })
  assert.deepEqual(visibleShortcuts(['VIDEO_MANAGE', 'CONTENT_VIEW']), {
    createArticle: false,
    createVideo: true,
    contents: true,
    categories: false,
  })
})

test('生产路由权限守卫', () => {
  assert.equal(access.canAccessRoute(['CONTENT_CREATE'], access.routePermissions({ permissions: ['CONTENT_CREATE'] })), true)
  assert.equal(access.canAccessRoute(['CONTENT_VIEW'], access.routePermissions({ permissions: ['CONTENT_CREATE'] })), false)
  assert.equal(access.canAccessRoute(['VIDEO_MANAGE'], access.routePermissions({ permissions: ['VIDEO_MANAGE'] })), true)
  assert.equal(access.routePermissions({}), null)
})

test('Dashboard 页面调用 dashboardApi 而非直接 axios', () => {
  const page = fs.readFileSync(path.resolve('src/views/dashboard/index.vue'), 'utf8')
  const api = fs.readFileSync(path.resolve('src/api/dashboard.ts'), 'utf8')
  assert.match(page, /dashboardApi/)
  assert.equal(page.includes('axios.get'), false)
  assert.match(api, /\/admin\/api\/dashboard\/overview/)
})

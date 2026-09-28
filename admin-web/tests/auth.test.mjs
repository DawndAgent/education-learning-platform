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

const allPermissions = [
  'CONTENT_VIEW',
  'CONTENT_CREATE',
  'CONTENT_UPDATE',
  'CONTENT_DELETE',
  'CONTENT_PUBLISH',
  'CONTENT_OFFLINE',
  'CATEGORY_MANAGE',
  'VIDEO_MANAGE',
  'DASHBOARD_VIEW'
]

test('未登录访问后台会去登录页', () => {
  assert.equal(access.nextRoute('/dashboard', true, false), '/login')
  assert.equal(access.nextRoute('/categories', true, false), '/login')
  assert.equal(access.nextRoute('/login', false, false), null)
})

test('已登录再访问登录页会去首页', () => {
  assert.equal(access.nextRoute('/login', false, true), '/dashboard')
  assert.equal(access.nextRoute('/dashboard', true, true), null)
})

test('登录接口 401 不触发离开登录页', () => {
  assert.equal(access.shouldLeaveAfterUnauthorized('/admin/api/auth/login'), false)
  assert.equal(access.shouldLeaveAfterUnauthorized('/admin/api/auth/me'), true)
})

test('菜单按权限显示', () => {
  assert.equal(access.canSeeMenu('/dashboard', []), false)
  assert.equal(access.canSeeMenu('/dashboard', ['DASHBOARD_VIEW']), true)
  assert.equal(access.canSeeMenu('/categories', ['CONTENT_VIEW']), false)
  assert.equal(access.canSeeMenu('/categories', ['CATEGORY_MANAGE']), true)
  assert.equal(access.canSeeMenu('/contents', ['CONTENT_VIEW']), true)
  assert.equal(access.canSeeMenu('/articles', ['CONTENT_VIEW']), true)
  assert.equal(access.canSeeMenu('/videos', ['CONTENT_VIEW']), false)
  assert.equal(access.canSeeMenu('/videos', allPermissions), true)
})

test('登录页保存的是 token 而不是密码', () => {
  const tokenSource = fs.readFileSync(path.resolve('src/utils/token.ts'), 'utf8')
  const loginSource = fs.readFileSync(path.resolve('src/views/login/index.vue'), 'utf8')
  assert.match(tokenSource, /admin_token/)
  assert.equal(loginSource.includes('localStorage'), false)
  assert.match(loginSource, /authStore\.login/)
})

test('编辑子路由侧栏高亮到一级菜单', () => {
  assert.equal(access.activeMenuPath('/dashboard'), '/dashboard')
  assert.equal(access.activeMenuPath('/contents'), '/contents')
  assert.equal(access.activeMenuPath('/articles'), '/articles')
  assert.equal(access.activeMenuPath('/articles/create'), '/articles')
  assert.equal(access.activeMenuPath('/articles/10001'), '/articles')
  assert.equal(access.activeMenuPath('/videos/create'), '/videos')
  assert.equal(access.activeMenuPath('/videos/20002'), '/videos')
})

test('401 离开逻辑与登录页排除', () => {
  assert.equal(access.shouldLeaveAfterUnauthorized('/admin/api/dashboard/overview'), true)
  assert.equal(access.shouldLeaveAfterUnauthorized('/admin/api/auth/login'), false)
})

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

const form = require('../src/utils/topic-form.ts')
const access = require('../src/utils/access.ts')

const listSource = fs.readFileSync(path.resolve('src/views/topics/index.vue'), 'utf8')
const editorSource = fs.readFileSync(path.resolve('src/views/topics/editor.vue'), 'utf8')
const apiSource = fs.readFileSync(path.resolve('src/api/topic.ts'), 'utf8')
const layoutSource = fs.readFileSync(path.resolve('src/layouts/AdminLayout.vue'), 'utf8')
const routerSource = fs.readFileSync(path.resolve('src/router/index.ts'), 'utf8')

const allPermissions = [
  'TOPIC_VIEW',
  'TOPIC_CREATE',
  'TOPIC_UPDATE',
  'TOPIC_DELETE',
  'TOPIC_PUBLISH',
  'TOPIC_OFFLINE',
  'TOPIC_CONTENT_MANAGE'
]

test('专题菜单与路由权限', () => {
  assert.equal(access.canSeeMenu('/topics', []), false)
  assert.equal(access.canSeeMenu('/topics', ['TOPIC_VIEW']), true)
  assert.equal(access.activeMenuPath('/topics/create'), '/topics')
  assert.equal(access.activeMenuPath('/topics/9/edit'), '/topics')
  assert.equal(access.canAccessRoute(['TOPIC_CREATE'], access.routePermissions({ permissions: ['TOPIC_CREATE'] })), true)
  assert.equal(access.canAccessRoute(['TOPIC_VIEW'], access.routePermissions({ permissions: ['TOPIC_CREATE'] })), false)
})

test('操作按钮随状态和权限变化', () => {
  assert.deepEqual(form.visibleActions('DRAFT', allPermissions), {
    edit: true, manageContents: true, preview: true, publish: true, offline: false, remove: true
  })
  assert.deepEqual(form.visibleActions('PUBLISHED', allPermissions), {
    edit: true, manageContents: true, preview: true, publish: false, offline: true, remove: false
  })
  assert.deepEqual(form.visibleActions('OFFLINE', allPermissions), {
    edit: true, manageContents: true, preview: true, publish: true, offline: false, remove: true
  })
  assert.equal(form.visibleActions('DRAFT', ['TOPIC_VIEW']).remove, false)
  assert.equal(form.visibleActions('DRAFT', ['TOPIC_VIEW']).manageContents, false)
  assert.equal(form.publishActionLabel('OFFLINE'), '重新发布')
  assert.equal(form.deleteConfirmText('词汇'), '确定删除专题「词汇」吗？')
  assert.equal(form.publishConfirmText('词汇'), '确定发布「词汇」吗？发布后该专题将在小程序端可见。')
  assert.equal(form.offlineConfirmText('词汇'), '确认下线「词汇」？')
})

test('表单校验与载荷', () => {
  const base = {
    name: 'KET 词汇',
    code: 'KET_VOCAB',
    coverUrl: '',
    summary: '',
    categoryId: '11',
    sort: 1
  }
  assert.equal(form.validateTopicForm(base, ['11'], true), null)
  assert.equal(form.validateTopicForm({ ...base, name: ' ' }, ['11'], true), '专题名称不能为空')
  assert.equal(form.validateTopicForm({ ...base, code: 'bad' }, ['11'], true), '专题编码必须是大写字母、数字或下划线')
  assert.equal(form.validateTopicForm({ ...base, categoryId: '1' }, ['11'], true), '专题只能选择二级分类')
  assert.equal(form.toCreatePayload(base).code, 'KET_VOCAB')
  assert.equal(form.toUpdatePayload(base).name, 'KET 词汇')
  assert.deepEqual(form.toSortPayload([{ contentId: 'a' }, { contentId: 'b' }]), [
    { contentId: 'a', sort: 0 },
    { contentId: 'b', sort: 1 }
  ])
  assert.deepEqual(form.moveContentItem(['a', 'b', 'c'], 1, -1), ['b', 'a', 'c'])
})

test('专题 API 路径与页面不直接使用 axios/wx.request', () => {
  assert.match(apiSource, /\/admin\/api\/topics/)
  assert.match(apiSource, /\/admin\/api\/topics\/\$\{id\}\/publish/)
  assert.match(apiSource, /\/admin\/api\/topics\/\$\{id\}\/offline/)
  assert.match(apiSource, /\/admin\/api\/topics\/\$\{topicId\}\/contents/)
  assert.match(apiSource, /\/admin\/api\/topics\/\$\{topicId\}\/contents\/sort/)
  assert.match(apiSource, /getTopicList/)
  assert.match(apiSource, /createTopic/)
  assert.match(apiSource, /addTopicContents/)
  assert.match(apiSource, /sortTopicContents/)
  assert.equal(listSource.includes("from 'axios'"), false)
  assert.equal(editorSource.includes("from 'axios'"), false)
  assert.equal(listSource.includes('wx.request'), false)
  assert.equal(editorSource.includes('wx.request'), false)
  assert.match(listSource, /topicApi\./)
  assert.match(editorSource, /topicApi\./)
  assert.match(editorSource, /contentApi\.getContentList/)
  assert.match(listSource, /ElMessageBox\.confirm/)
  assert.match(editorSource, /ElMessageBox\.confirm/)
  assert.match(layoutSource, /专题管理/)
  assert.match(layoutSource, /\/topics/)
  assert.match(routerSource, /topics\/create/)
  assert.match(routerSource, /topics\/:id\/edit/)
  assert.match(routerSource, /TOPIC_CREATE/)
})

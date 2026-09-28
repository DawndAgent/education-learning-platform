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

const form = require('../src/utils/content-form.ts')
const pageSource = fs.readFileSync(path.resolve('src/views/contents/index.vue'), 'utf8')
const apiSource = fs.readFileSync(path.resolve('src/api/content.ts'), 'utf8')

const tree = [
  {
    id: '1',
    parentId: '0',
    name: '剑桥英语',
    code: 'CAMBRIDGE',
    iconUrl: null,
    description: null,
    sort: 1,
    status: 'ENABLED',
    children: [
      { id: '11', parentId: '1', name: 'KET/PET备考资料', code: 'CAMBRIDGE_KET', iconUrl: null, description: null, sort: 1, status: 'ENABLED' }
    ]
  }
]

const allPermissions = [
  'CONTENT_VIEW',
  'CONTENT_CREATE',
  'CONTENT_UPDATE',
  'CONTENT_DELETE',
  'CONTENT_PUBLISH',
  'CONTENT_OFFLINE',
  'VIDEO_MANAGE'
]

test('类型和状态显示中文', () => {
  assert.equal(form.contentTypeLabel('ARTICLE'), '文章')
  assert.equal(form.contentTypeLabel('VIDEO'), '视频')
  assert.equal(form.statusLabel('DRAFT'), '草稿')
  assert.equal(form.statusLabel('PUBLISHED'), '已发布')
  assert.equal(form.statusLabel('OFFLINE'), '已下架')
})

test('分类选项区分一级和二级', () => {
  const options = form.categoryOptions(tree)
  assert.equal(options[0].leaf, false)
  assert.equal(options[1].leaf, true)
  assert.match(options[1].label, /KET\/PET备考资料/)
})

test('表单校验和保存草稿载荷', () => {
  const base = {
    title: '听力资料',
    contentType: 'ARTICLE',
    categoryId: '11',
    coverUrl: '',
    summary: '',
    sort: 0
  }
  assert.equal(form.validateContentForm(base, ['11']), null)
  assert.equal(form.validateContentForm({ ...base, title: ' ' }, ['11']), '标题不能为空')
  assert.equal(form.validateContentForm({ ...base, categoryId: '1' }, ['11']), '内容只能选择二级分类')
  assert.equal(form.validateContentForm({ ...base, contentType: '' }, ['11']), '请选择内容类型')
  assert.equal(form.toCreatePayload(base).contentType, 'ARTICLE')
  assert.equal(form.toUpdatePayload(base, 'ARTICLE').contentType, 'ARTICLE')
})

test('搜索回到第一页，操作按钮随状态和权限变化', () => {
  const next = form.searchQuery({ pageNum: 3, pageSize: 10, keyword: '英语', categoryId: '1', contentType: 'ARTICLE', status: 'DRAFT' })
  assert.equal(next.pageNum, 1)
  assert.equal(next.keyword, '英语')
  assert.deepEqual(form.visibleActions('DRAFT', allPermissions, 'ARTICLE'), {
    edit: true, publish: true, offline: false, remove: true, copy: true, preview: true
  })
  assert.deepEqual(form.visibleActions('PUBLISHED', allPermissions, 'ARTICLE'), {
    edit: true, publish: false, offline: true, remove: false, copy: true, preview: true
  })
  assert.deepEqual(form.visibleActions('OFFLINE', allPermissions, 'ARTICLE'), {
    edit: true, publish: true, offline: false, remove: true, copy: true, preview: true
  })
  assert.equal(form.visibleActions('DRAFT', ['CONTENT_VIEW']).remove, false)
  assert.equal(form.visibleActions('DRAFT', ['CONTENT_VIEW']).copy, false)
  assert.equal(form.canDuplicate('VIDEO', ['CONTENT_CREATE']), false)
  assert.equal(form.canDuplicate('VIDEO', ['CONTENT_CREATE', 'VIDEO_MANAGE']), true)
  assert.equal(form.deleteConfirmText('听力'), '确定删除内容「听力」吗？')
  assert.equal(form.publishConfirmText('听力'), '确认发布《听力》\n发布后该内容将在小程序端可见。')
  assert.equal(form.offlineConfirmText('听力'), '确认下线「听力」？')
  assert.equal(form.batchOfflineConfirmText(3), '确定要下线选中的 3 条内容吗？')
  assert.equal(form.selectableForBatchOffline([{ status: 'PUBLISHED' }, { status: 'PUBLISHED' }]), true)
  assert.equal(form.selectableForBatchOffline([{ status: 'PUBLISHED' }, { status: 'DRAFT' }]), false)
  assert.equal(form.selectableForBatchDelete([{ status: 'DRAFT' }, { status: 'OFFLINE' }]), true)
  assert.equal(form.selectableForBatchDelete([{ status: 'PUBLISHED' }]), false)
  assert.equal(form.nextPageAfterDelete(3, 10, 21, 1), 2)
  assert.equal(form.nextPageAfterDelete(1, 10, 1, 1), 1)
  assert.equal(form.canOpenEditor('ARTICLE', ['CONTENT_UPDATE']), true)
  assert.equal(form.canOpenEditor('VIDEO', ['CONTENT_UPDATE']), false)
  assert.equal(form.canOpenEditor('VIDEO', ['VIDEO_MANAGE']), true)
  assert.equal(form.editorPath('ARTICLE', '9'), '/articles/9')
  assert.equal(form.editorPath('VIDEO', '8'), '/videos/8')
  assert.equal(form.visibleActions('DRAFT', ['CONTENT_UPDATE'], 'VIDEO').edit, false)
  assert.equal(form.visibleActions('DRAFT', ['VIDEO_MANAGE'], 'VIDEO').edit, true)
  assert.equal(form.saveSuccessMessage('DRAFT'), '已保存草稿')
  assert.equal(form.saveSuccessMessage('PUBLISHED'), '保存成功')
  assert.equal(form.publishActionLabel('OFFLINE'), '重新发布')
  assert.equal(form.publishActionLabel('DRAFT'), '发布')
})

test('内容页走封装接口并覆盖列表操作', () => {
  assert.match(apiSource, /getContentList/)
  assert.match(apiSource, /getContentDetail/)
  assert.match(apiSource, /createContent/)
  assert.match(apiSource, /updateContent/)
  assert.match(apiSource, /deleteContent/)
  assert.match(apiSource, /publishContent/)
  assert.match(apiSource, /offlineContent/)
  assert.match(apiSource, /duplicateContent/)
  assert.match(apiSource, /batchOfflineContents/)
  assert.match(apiSource, /batchDeleteContents/)
  assert.equal(pageSource.includes("from 'axios'"), false)
  assert.match(pageSource, /contentApi\./)
  assert.match(pageSource, /ElMessageBox\.confirm/)
  assert.match(pageSource, /暂无内容/)
  assert.match(pageSource, /publishingId/)
  assert.match(pageSource, /offliningId/)
  assert.match(pageSource, /deletingId/)
  assert.match(pageSource, /copyingId/)
  assert.match(pageSource, /批量下线/)
  assert.match(pageSource, /批量删除/)
  assert.match(pageSource, /ContentPreviewDialog/)
  assert.match(pageSource, /el-pagination/)
  assert.match(pageSource, /新增文章/)
  assert.match(pageSource, /新增视频/)
  assert.match(pageSource, /publishConfirmText/)
  assert.match(fs.readFileSync(path.resolve('src/utils/content-form.ts'), 'utf8'), /发布后该内容将在小程序端可见/)
})

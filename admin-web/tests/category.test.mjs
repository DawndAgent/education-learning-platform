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

const form = require('../src/utils/category-form.ts')
const access = require('../src/utils/access.ts')

const pageSource = fs.readFileSync(path.resolve('src/views/categories/index.vue'), 'utf8')
const apiSource = fs.readFileSync(path.resolve('src/api/category.ts'), 'utf8')

const cambridge = {
  id: '1',
  parentId: '0',
  name: '剑桥英语',
  code: 'CAMBRIDGE',
  iconUrl: null,
  description: null,
  sort: 1,
  status: 'ENABLED',
  children: [
    { id: '12', parentId: '1', name: '剑桥原版阅读', code: 'CAMBRIDGE_READING', iconUrl: null, description: null, sort: 2, status: 'ENABLED', children: [] },
    { id: '11', parentId: '1', name: 'KET/PET备考资料', code: 'CAMBRIDGE_KET', iconUrl: null, description: null, sort: 1, status: 'ENABLED' }
  ]
}

const math = {
  id: '2',
  parentId: '0',
  name: '数学思维',
  code: 'MATH',
  iconUrl: null,
  description: null,
  sort: 2,
  status: 'ENABLED',
  children: []
}

test('分类树按 sort 和 id 升序展示', () => {
  const tree = form.normalizeTree([math, cambridge])
  assert.deepEqual(tree.map((item) => item.code), ['CAMBRIDGE', 'MATH'])
  assert.deepEqual(tree[0].children.map((item) => item.code), ['CAMBRIDGE_KET', 'CAMBRIDGE_READING'])
  assert.equal(tree[1].children, undefined)
})

test('父级只能选择无和一级分类', () => {
  const choices = form.parentChoices(form.normalizeTree([cambridge, math]), '1')
  assert.deepEqual(choices.map((item) => item.name), ['无', '数学思维'])
  assert.equal(form.canAddChild(cambridge), true)
  assert.equal(form.canAddChild(cambridge.children[0]), false)
})

test('表单校验名称、编码和排序', () => {
  const base = {
    name: '剑桥英语',
    code: 'CAMBRIDGE',
    parentId: '0',
    iconUrl: '',
    description: '',
    sort: 1,
    status: 'ENABLED'
  }
  assert.equal(form.validateCategoryForm(base), null)
  assert.equal(form.validateCategoryForm({ ...base, name: '  ' }), '分类名称不能为空')
  assert.equal(form.validateCategoryForm({ ...base, code: 'bad' }), '分类编码必须是大写字母、数字或下划线')
  assert.equal(form.validateCategoryForm({ ...base, sort: null }), '排序不能为空')
  assert.equal(form.toPayload(base).status, 'ENABLED')
})

test('删除和启停确认文案', () => {
  assert.equal(form.deleteConfirmText('剑桥英语'), '确定删除分类「剑桥英语」吗？')
  assert.equal(form.statusConfirmText('剑桥英语', 'DISABLED'), '确定停用分类「剑桥英语」吗？')
  assert.equal(form.statusConfirmText('剑桥英语', 'ENABLED'), '确定启用分类「剑桥英语」吗？')
  assert.equal(form.nextStatus('ENABLED'), 'DISABLED')
})

test('分类页走封装接口并处理确认与错误', () => {
  assert.match(apiSource, /getCategoryTree/)
  assert.match(apiSource, /createCategory/)
  assert.match(apiSource, /updateCategory/)
  assert.match(apiSource, /deleteCategory/)
  assert.equal(pageSource.includes("from 'axios'"), false)
  assert.match(pageSource, /categoryApi\./)
  assert.match(pageSource, /ElMessageBox\.confirm/)
  assert.match(pageSource, /暂无分类/)
  assert.match(pageSource, /loadError/)
  assert.match(pageSource, /:loading="submitting"/)
  assert.equal(access.canSeeMenu('/categories', ['CONTENT_VIEW']), false)
  assert.equal(access.canSeeMenu('/categories', ['CATEGORY_MANAGE']), true)
})

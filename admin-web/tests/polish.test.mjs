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

const toast = require('../src/utils/error-toast.ts')

test('相同错误提示在短窗口内去重', () => {
  toast.resetErrorToastWindow()
  assert.equal(toast.shouldShowErrorToast('网络错误', 1000), true)
  assert.equal(toast.shouldShowErrorToast('网络错误', 1200), false)
  assert.equal(toast.shouldShowErrorToast('网络错误', 1000 + toast.ERROR_TOAST_DEDUP_MS), true)
})

test('不同错误提示不去重', () => {
  toast.resetErrorToastWindow()
  assert.equal(toast.shouldShowErrorToast('没有权限', 1000), true)
  assert.equal(toast.shouldShowErrorToast('网络错误', 1100), true)
})

test('401 处理会清理 auth store', () => {
  const source = fs.readFileSync(path.resolve('src/api/request.ts'), 'utf8')
  assert.match(source, /clearSession/)
  assert.match(source, /shouldShowErrorToast/)
  assert.match(source, /status === 403/)
})

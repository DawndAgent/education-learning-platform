import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const miniRoot = path.join(root, 'miniprogram')
const failures = []

const appJson = JSON.parse(fs.readFileSync(path.join(miniRoot, 'app.json'), 'utf8'))
for (const page of appJson.pages) {
  for (const ext of ['.ts', '.json', '.wxml', '.wxss']) {
    const file = path.join(miniRoot, `${page}${ext}`)
    if (!fs.existsSync(file)) {
      failures.push(`missing page file ${page}${ext}`)
    }
  }
}

const project = JSON.parse(fs.readFileSync(path.join(root, 'project.config.json'), 'utf8'))
const plugins = project.setting && project.setting.useCompilerPlugins
if (!Array.isArray(plugins) || !plugins.includes('typescript')) {
  failures.push('project.config.json must enable the typescript compiler plugin')
}

walk(miniRoot, (file) => {
  if (!file.endsWith('.ts') && !file.endsWith('.wxml') && !file.endsWith('.json')) {
    return
  }
  const text = fs.readFileSync(file, 'utf8')
  const relative = path.relative(root, file).replaceAll('\\', '/')
  if (text.includes('TODO') || text.includes('FIXME')) {
    failures.push(`${relative} contains TODO or FIXME`)
  }
  if (text.includes('wx.request') && relative !== 'miniprogram/services/request.ts') {
    failures.push(`${relative} calls wx.request directly`)
  }
  if (relative !== 'miniprogram/config/env.ts' && /https?:\/\//.test(text)) {
    failures.push(`${relative} hardcodes an API address`)
  }
})

if (failures.length > 0) {
  for (const failure of failures) {
    console.error(failure)
  }
  process.exit(1)
}

console.log(`miniprogram structure ok, pages=${appJson.pages.length}`)

function walk(directory, visit) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const fullPath = path.join(directory, entry.name)
    if (entry.isDirectory()) {
      walk(fullPath, visit)
    } else {
      visit(fullPath)
    }
  }
}

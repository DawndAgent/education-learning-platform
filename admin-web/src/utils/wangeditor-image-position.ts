import {
  Boot,
  DomEditor,
  SlateElement,
  SlateNode,
  SlateTransforms,
  type IDomEditor,
} from '@wangeditor/editor'
import {
  buildImageStyleAttr,
  readDomCssLength,
  type ImagePositionStyle,
} from '@/utils/image-position-style'

export type { ImagePositionStyle } from '@/utils/image-position-style'
export { buildImageStyleAttr, readCssLength } from '@/utils/image-position-style'

type ImageElement = {
  type: 'image'
  src: string
  alt?: string
  href?: string
  style?: ImagePositionStyle
  children: [{ text: '' }]
}

type PositionRule = {
  ml: number
  mt: number
  tx: number
  ty: number
}

const STYLE_TAG_ID = 'w-e-image-free-position'

let registered = false

/**
 * wangEditor 默认图片 HTML 只保留宽高；覆盖序列化/解析以支持自由拖拽偏移。
 */
export function registerWangEditorImagePosition(): void {
  if (registered) {
    return
  }
  registered = true

  Boot.registerModule({
    elemsToHtml: [
      {
        type: 'image',
        elemToHtml(elem) {
          const node = elem as ImageElement
          const style = node.style || {}
          const alt = node.alt || ''
          const href = node.href || ''
          const styleAttr = buildImageStyleAttr(style)
          return `<img src="${node.src}" alt="${alt}" data-href="${href}" style="${styleAttr}"/>`
        },
      },
    ],
    parseElemsHtml: [
      {
        selector: 'img:not([data-w-e-type])',
        parseElemHtml(domElem) {
          const el = domElem as Element
          let href = el.getAttribute('data-href') || ''
          try {
            href = decodeURIComponent(href)
          } catch {
            // keep raw href
          }
          return {
            type: 'image',
            src: el.getAttribute('src') || '',
            alt: el.getAttribute('alt') || '',
            href,
            style: {
              width: readDomCssLength(el, 'width'),
              height: readDomCssLength(el, 'height'),
              marginLeft: readDomCssLength(el, 'margin-left'),
              marginTop: readDomCssLength(el, 'margin-top'),
            },
            children: [{ text: '' }],
          } as ImageElement
        },
      },
    ],
  })
}

function parsePx(value: string | undefined): number {
  if (!value) {
    return 0
  }
  const n = Number.parseFloat(value)
  return Number.isFinite(n) ? n : 0
}

function formatPx(value: number): string {
  return `${Math.round(value)}px`
}

function ensureStyleTag(): HTMLStyleElement {
  let el = document.getElementById(STYLE_TAG_ID) as HTMLStyleElement | null
  if (!el) {
    el = document.createElement('style')
    el.id = STYLE_TAG_ID
    document.head.appendChild(el)
  }
  return el
}

function imageContainerId(editor: IDomEditor, node: ImageElement): string {
  const key = DomEditor.findKey(editor, node)
  return `w-e-image-container-${key.id}`
}

/**
 * 用 stylesheet（!important）定位，避免 snabbdom 重绘时清掉 inline margin/transform。
 */
function flushPositionStyles(rules: Map<string, PositionRule>): void {
  const styleEl = ensureStyleTag()
  const css: string[] = []
  rules.forEach((rule, id) => {
    const parts = [
      `margin-left:${Math.round(rule.ml)}px !important`,
      `margin-top:${Math.round(rule.mt)}px !important`,
    ]
    if (rule.tx || rule.ty) {
      parts.push(`transform:translate(${Math.round(rule.tx)}px,${Math.round(rule.ty)}px) !important`)
    }
    css.push(`#${CSS.escape(id)}{${parts.join(';')}}`)
  })
  styleEl.textContent = css.join('')
}

function collectRulesFromEditor(editor: IDomEditor): Map<string, PositionRule> {
  const rules = new Map<string, PositionRule>()
  for (const [node] of SlateNode.nodes(editor)) {
    if (!SlateElement.isElement(node) || (node as ImageElement).type !== 'image') {
      continue
    }
    const image = node as ImageElement
    const style = image.style || {}
    const ml = parsePx(style.marginLeft)
    const mt = parsePx(style.marginTop)
    if (!ml && !mt) {
      continue
    }
    rules.set(imageContainerId(editor, image), { ml, mt, tx: 0, ty: 0 })
  }
  return rules
}

/**
 * 按住图片拖动即可自由移动；拖拽中用 transform，松手写入 margin 到 HTML。
 */
export function bindImageFreeMove(editor: IDomEditor): () => void {
  const root = editor.getEditableContainer()
  if (!root) {
    return () => undefined
  }

  const rules = new Map<string, PositionRule>()
  let dragging = false
  let moved = false
  let startX = 0
  let startY = 0
  let containerId = ''
  let imagePath: number[] | null = null
  let baseStyle: ImagePositionStyle = {}
  let paintRaf = 0
  let syncRaf = 0
  let pendingDx = 0
  let pendingDy = 0

  const publish = () => {
    flushPositionStyles(rules)
  }

  const syncFromEditor = () => {
    if (dragging) {
      return
    }
    const next = collectRulesFromEditor(editor)
    // 拖拽中途不要丢掉当前 id 的临时 translate；非拖拽时全量同步
    rules.clear()
    next.forEach((value, key) => rules.set(key, value))
    publish()
  }

  const scheduleSync = () => {
    if (dragging) {
      return
    }
    cancelAnimationFrame(syncRaf)
    syncRaf = requestAnimationFrame(() => {
      syncFromEditor()
      // snabbdom patch 可能再晚一帧
      requestAnimationFrame(() => {
        if (!dragging) {
          syncFromEditor()
        }
      })
    })
  }

  const paintDrag = () => {
    paintRaf = 0
    if (!dragging || !containerId) {
      return
    }
    const current = rules.get(containerId) || { ml: 0, mt: 0, tx: 0, ty: 0 }
    current.tx = pendingDx
    current.ty = pendingDy
    rules.set(containerId, current)
    publish()
  }

  const onMouseMove = (event: MouseEvent) => {
    if (!dragging) {
      return
    }
    pendingDx = event.clientX - startX
    pendingDy = event.clientY - startY
    if (!moved) {
      if (Math.abs(pendingDx) < 2 && Math.abs(pendingDy) < 2) {
        return
      }
      moved = true
    }
    event.preventDefault()
    if (!paintRaf) {
      paintRaf = requestAnimationFrame(paintDrag)
    }
  }

  const onMouseUp = (event: MouseEvent) => {
    if (!dragging) {
      return
    }
    dragging = false
    document.removeEventListener('mousemove', onMouseMove, true)
    document.removeEventListener('mouseup', onMouseUp, true)
    cancelAnimationFrame(paintRaf)
    paintRaf = 0

    const dx = event.clientX - startX
    const dy = event.clientY - startY
    const didMove = moved || Math.abs(dx) >= 2 || Math.abs(dy) >= 2
    const path = imagePath
    const id = containerId
    imagePath = null
    containerId = ''

    if (!didMove || !path || !id) {
      const rule = rules.get(id)
      if (rule) {
        rule.tx = 0
        rule.ty = 0
        if (!rule.ml && !rule.mt) {
          rules.delete(id)
        }
      }
      publish()
      scheduleSync()
      return
    }

    const prev = rules.get(id) || { ml: 0, mt: 0, tx: 0, ty: 0 }
    const nextMl = prev.ml + dx
    const nextMt = prev.mt + dy
    const nextStyle: ImagePositionStyle = {
      ...baseStyle,
      marginLeft: formatPx(nextMl),
      marginTop: formatPx(nextMt),
    }
    rules.set(id, { ml: nextMl, mt: nextMt, tx: 0, ty: 0 })
    publish()

    try {
      SlateTransforms.setNodes(editor, { style: nextStyle } as Partial<ImageElement>, { at: path })
      if (path.length > 1) {
        SlateTransforms.setNodes(editor, { textAlign: 'left' } as Record<string, string>, {
          at: path.slice(0, -1),
        })
      }
    } catch {
      // path 失效时至少保留 stylesheet 位置
    }
    scheduleSync()
  }

  const onDragStart = (event: Event) => {
    const target = event.target as HTMLElement | null
    if (target?.closest('.w-e-image-container')) {
      // 阻止浏览器默认拖图片（否则会吃掉 mousemove，表现为“拖不动”）
      event.preventDefault()
    }
  }

  const onMouseDown = (event: Event) => {
    const mouse = event as MouseEvent
    if (mouse.button !== 0 || editor.isDisabled()) {
      return
    }
    const target = mouse.target as HTMLElement | null
    if (!target || target.closest('.w-e-image-dragger')) {
      return
    }
    const box = target.closest('.w-e-image-container') as HTMLElement | null
    if (!box) {
      return
    }
    const img = box.querySelector('img')
    if (!img) {
      return
    }

    let node: ImageElement
    let path: number[]
    let id = box.id
    try {
      node = DomEditor.toSlateNode(editor, img) as ImageElement
      if (node.type !== 'image') {
        return
      }
      path = DomEditor.findPath(editor, node) as number[]
      id = imageContainerId(editor, node) || box.id
    } catch {
      return
    }
    if (!id) {
      return
    }

    const style = node.style || {}
    const existing = rules.get(id)
    const ml = existing?.ml ?? parsePx(style.marginLeft)
    const mt = existing?.mt ?? parsePx(style.marginTop)

    dragging = true
    moved = false
    containerId = id
    imagePath = path
    baseStyle = { ...style }
    startX = mouse.clientX
    startY = mouse.clientY
    pendingDx = 0
    pendingDy = 0
    rules.set(id, { ml, mt, tx: 0, ty: 0 })
    publish()

    document.addEventListener('mousemove', onMouseMove, true)
    document.addEventListener('mouseup', onMouseUp, true)
  }

  root.addEventListener('dragstart', onDragStart, true)
  root.addEventListener('mousedown', onMouseDown, true)

  const originalApply = editor.apply.bind(editor)
  editor.apply = ((op: unknown) => {
    originalApply(op as never)
    scheduleSync()
  }) as typeof editor.apply

  syncFromEditor()

  return () => {
    cancelAnimationFrame(paintRaf)
    cancelAnimationFrame(syncRaf)
    root.removeEventListener('dragstart', onDragStart, true)
    root.removeEventListener('mousedown', onMouseDown, true)
    document.removeEventListener('mousemove', onMouseMove, true)
    document.removeEventListener('mouseup', onMouseUp, true)
    editor.apply = originalApply as typeof editor.apply
    // 不移除全局 style 标签，避免多编辑器互相影响；清空当前规则即可
    rules.clear()
    publish()
  }
}

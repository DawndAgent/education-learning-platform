export type ImagePositionStyle = {
  width?: string
  height?: string
  marginLeft?: string
  marginTop?: string
}

const LENGTH_RE = /^-?\d+(\.\d+)?(px|%|em|rem|vw)?$/i

export function readCssLength(styleText: string, prop: string): string {
  if (!styleText) {
    return ''
  }
  const matched = styleText.match(new RegExp(`${prop}\\s*:\\s*([^;]+)`, 'i'))
  if (!matched) {
    return ''
  }
  const value = matched[1].trim()
  return LENGTH_RE.test(value) ? value : ''
}

export function buildImageStyleAttr(style: ImagePositionStyle = {}): string {
  const parts: string[] = []
  if (style.width) {
    parts.push(`width: ${style.width};`)
  }
  if (style.height) {
    parts.push(`height: ${style.height};`)
  }
  if (style.marginLeft) {
    parts.push(`margin-left: ${style.marginLeft};`)
  }
  if (style.marginTop) {
    parts.push(`margin-top: ${style.marginTop};`)
  }
  return parts.join('')
}

export function readDomCssLength(el: Element, prop: string): string {
  const attr = el.getAttribute('style') || ''
  const fromAttr = readCssLength(attr, prop)
  if (fromAttr) {
    return fromAttr
  }
  const camel = prop.replace(/-([a-z])/g, (_, c: string) => c.toUpperCase()) as keyof CSSStyleDeclaration
  const inline = (el as HTMLElement).style?.[camel]
  return typeof inline === 'string' && LENGTH_RE.test(inline) ? inline : ''
}

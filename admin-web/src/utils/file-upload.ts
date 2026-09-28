const MAX_IMAGE_BYTES = 10 * 1024 * 1024
const MAX_DOCUMENT_BYTES = 20 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif'])
const ALLOWED_DOCUMENT_EXTENSIONS = new Set(['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'])

export function validateImageFile(file: { size: number; type: string }, maxBytes = MAX_IMAGE_BYTES): string | null {
  if (!file || file.size <= 0) {
    return '文件不能为空'
  }
  if (file.size > maxBytes) {
    return '文件大小不能超过10MB'
  }
  if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
    return '仅支持 JPEG、PNG、WEBP、GIF 图片'
  }
  return null
}

export function documentExtension(fileName: string): string {
  const name = fileName.replace(/\\/g, '/')
  const base = name.includes('/') ? name.slice(name.lastIndexOf('/') + 1) : name
  const dot = base.lastIndexOf('.')
  if (dot < 0 || dot === base.length - 1) {
    return ''
  }
  return base.slice(dot + 1).toLowerCase()
}

export function validateDocumentFile(
  file: { name: string; size: number },
  maxBytes = MAX_DOCUMENT_BYTES,
): string | null {
  if (!file || file.size <= 0) {
    return '文件不能为空'
  }
  if (file.size > maxBytes) {
    return '文件大小不能超过20MB'
  }
  const extension = documentExtension(file.name)
  if (!ALLOWED_DOCUMENT_EXTENSIONS.has(extension)) {
    return '仅支持 PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX 文件'
  }
  return null
}

export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes == null || !Number.isFinite(bytes) || bytes < 0) {
    return ''
  }
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

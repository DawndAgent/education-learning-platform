import type { Category, CategoryPayload, CategoryStatus } from '../types/category'

export const CODE_PATTERN = /^[A-Z][A-Z0-9_]{1,63}$/

export interface CategoryFormValues {
  name: string
  code: string
  parentId: string
  iconUrl: string
  description: string
  sort: number | null
  status: CategoryStatus | ''
}

export function isRoot(category: Pick<Category, 'parentId'>): boolean {
  return category.parentId === '0'
}

export function canAddChild(category: Category): boolean {
  return isRoot(category)
}

export function parentChoices(tree: Category[], editingId = ''): Array<{ id: string; name: string }> {
  const choices = [{ id: '0', name: '无' }]
  for (const root of tree) {
    if (!isRoot(root) || root.id === editingId) {
      continue
    }
    choices.push({ id: root.id, name: root.name })
  }
  return choices
}

export function deleteConfirmText(name: string): string {
  return `确定删除分类「${name}」吗？`
}

export function statusConfirmText(name: string, next: CategoryStatus): string {
  if (next === 'ENABLED') {
    return `确定启用分类「${name}」吗？`
  }
  return `确定停用分类「${name}」吗？`
}

export function nextStatus(status: CategoryStatus): CategoryStatus {
  return status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
}

export function statusLabel(status: CategoryStatus): string {
  return status === 'ENABLED' ? '启用' : '停用'
}

export function compareCategory(left: Category, right: Category): number {
  if (left.sort !== right.sort) {
    return left.sort - right.sort
  }
  return compareId(left.id, right.id)
}

function compareId(left: string, right: string): number {
  try {
    const leftId = BigInt(left)
    const rightId = BigInt(right)
    if (leftId < rightId) {
      return -1
    }
    if (leftId > rightId) {
      return 1
    }
    return 0
  } catch {
    return left < right ? -1 : left > right ? 1 : 0
  }
}

export function normalizeTree(nodes: Category[]): Category[] {
  return [...nodes].sort(compareCategory).map((node) => {
    const children = node.children?.length ? normalizeTree(node.children) : undefined
    return {
      id: node.id,
      parentId: node.parentId,
      name: node.name,
      code: node.code,
      iconUrl: node.iconUrl,
      description: node.description,
      sort: node.sort,
      status: node.status,
      ...(children ? { children } : {}),
    }
  })
}

export function validateCategoryForm(form: CategoryFormValues): string | null {
  const name = form.name.trim()
  if (!name) {
    return '分类名称不能为空'
  }
  if (name.length > 64) {
    return '分类名称长度不能超过64'
  }
  const code = form.code.trim()
  if (!code) {
    return '分类编码不能为空'
  }
  if (!CODE_PATTERN.test(code)) {
    return '分类编码必须是大写字母、数字或下划线'
  }
  if (form.parentId === '') {
    return '请选择父级分类'
  }
  if (form.sort === null || Number.isNaN(form.sort)) {
    return '排序不能为空'
  }
  if (!Number.isInteger(form.sort) || form.sort < 0 || form.sort > 9999) {
    return '排序范围为 0 到 9999'
  }
  if (form.status !== 'ENABLED' && form.status !== 'DISABLED') {
    return '请选择状态'
  }
  if (form.iconUrl.length > 255) {
    return '图标地址长度不能超过255'
  }
  if (form.description.length > 255) {
    return '分类说明长度不能超过255'
  }
  return null
}

export function toPayload(form: CategoryFormValues): CategoryPayload {
  return {
    parentId: form.parentId,
    name: form.name.trim(),
    code: form.code.trim(),
    iconUrl: form.iconUrl.trim(),
    description: form.description.trim(),
    sort: form.sort ?? 0,
    status: form.status === 'DISABLED' ? 'DISABLED' : 'ENABLED',
  }
}

export function payloadFromCategory(category: Category, status: CategoryStatus): CategoryPayload {
  return {
    parentId: category.parentId,
    name: category.name,
    code: category.code,
    iconUrl: category.iconUrl ?? '',
    description: category.description ?? '',
    sort: category.sort,
    status,
  }
}

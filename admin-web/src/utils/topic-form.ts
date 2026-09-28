import type {
  TopicContentSortItem,
  TopicCreateRequest,
  TopicQuery,
  TopicStatus,
  TopicUpdateRequest,
} from '../types/topic'

/** 专题编码：大写字母开头，后接大写字母/数字/下划线，最长 100 */
export const TOPIC_CODE_PATTERN = /^[A-Z][A-Z0-9_]{0,99}$/

export interface TopicFormValues {
  name: string
  code: string
  coverUrl: string
  summary: string
  categoryId: string
  sort: number | null
}

export interface TopicActions {
  edit: boolean
  manageContents: boolean
  preview: boolean
  publish: boolean
  offline: boolean
  remove: boolean
}

export function visibleActions(status: TopicStatus, permissions: readonly string[]): TopicActions {
  const allowed = (permission: string) => permissions.includes(permission)
  return {
    edit: allowed('TOPIC_UPDATE') || allowed('TOPIC_VIEW'),
    manageContents: allowed('TOPIC_CONTENT_MANAGE'),
    preview: allowed('TOPIC_VIEW'),
    publish: status !== 'PUBLISHED' && allowed('TOPIC_PUBLISH'),
    offline: status === 'PUBLISHED' && allowed('TOPIC_OFFLINE'),
    remove: status !== 'PUBLISHED' && allowed('TOPIC_DELETE'),
  }
}

export function validateTopicForm(form: TopicFormValues, leaves: readonly string[], creating: boolean): string | null {
  const name = form.name.trim()
  if (!name) {
    return '专题名称不能为空'
  }
  if (name.length > 200) {
    return '专题名称长度不能超过200'
  }
  if (creating) {
    const code = form.code.trim()
    if (!code) {
      return '专题编码不能为空'
    }
    if (!TOPIC_CODE_PATTERN.test(code)) {
      return '专题编码必须是大写字母、数字或下划线'
    }
  }
  if (!form.categoryId) {
    return '请选择分类'
  }
  if (!leaves.includes(form.categoryId)) {
    return '专题只能选择二级分类'
  }
  if (form.sort === null || Number.isNaN(form.sort) || !Number.isInteger(form.sort) || form.sort < 0 || form.sort > 9999) {
    return '排序范围为 0 到 9999'
  }
  if (form.coverUrl.length > 500) {
    return '封面地址长度不能超过500'
  }
  if (form.summary.length > 500) {
    return '简介长度不能超过500'
  }
  return null
}

export function toCreatePayload(form: TopicFormValues): TopicCreateRequest {
  return {
    name: form.name.trim(),
    code: form.code.trim(),
    coverUrl: form.coverUrl.trim(),
    summary: form.summary.trim(),
    categoryId: form.categoryId,
    sort: form.sort ?? 0,
  }
}

export function toUpdatePayload(form: TopicFormValues): TopicUpdateRequest {
  return {
    name: form.name.trim(),
    coverUrl: form.coverUrl.trim(),
    summary: form.summary.trim(),
    categoryId: form.categoryId,
    sort: form.sort ?? 0,
  }
}

export function toSortPayload(items: readonly { contentId: string }[]): TopicContentSortItem[] {
  return items.map((item, index) => ({
    contentId: item.contentId,
    sort: index,
  }))
}

export function moveContentItem<T>(items: readonly T[], index: number, direction: -1 | 1): T[] {
  const target = index + direction
  if (index < 0 || target < 0 || index >= items.length || target >= items.length) {
    return [...items]
  }
  const next = [...items]
  const temp = next[index]
  next[index] = next[target]
  next[target] = temp
  return next
}

export function searchQuery(query: TopicQuery): TopicQuery {
  return { ...query, pageNum: 1 }
}

export function deleteConfirmText(name: string): string {
  return `确定删除专题「${name}」吗？`
}

export function publishConfirmText(name: string): string {
  return `确定发布「${name}」吗？发布后该专题将在小程序端可见。`
}

export function offlineConfirmText(name: string): string {
  return `确认下线「${name}」？`
}

export function removeContentConfirmText(title: string): string {
  return `确定从专题中移除「${title}」吗？`
}

export function saveSuccessMessage(status: TopicStatus): string {
  return status === 'DRAFT' ? '已保存草稿' : '保存成功'
}

export function publishActionLabel(status: TopicStatus): string {
  return status === 'OFFLINE' ? '重新发布' : '发布'
}

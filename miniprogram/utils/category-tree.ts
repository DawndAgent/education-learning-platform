import type { CategoryTreeVO } from '../types/category'

export interface HomeEntry {
  id: string
  name: string
  summary: string
  url: string
}

export interface CategoryChildLink {
  id: string
  name: string
  url: string
}

export interface CategoryPageModel {
  status: 'success' | 'empty' | 'error'
  message: string
  name: string
  children: CategoryChildLink[]
}

export interface RequestFailure {
  code: string
  httpStatus: number | null
  message: string
}

const ROOT_PARENT_ID = '0'

export function isRootParent(parentId: string | number | null | undefined): boolean {
  return parentId === ROOT_PARENT_ID || parentId === 0
}

export function isCategoryId(value: string): boolean {
  return /^\d{1,20}$/.test(value)
}

export function listRootCategories(tree: CategoryTreeVO[]): CategoryTreeVO[] {
  return tree.filter((item) => isRootParent(item.parentId)).sort(compareCategory)
}

export function listChildren(category: CategoryTreeVO | null): CategoryTreeVO[] {
  if (!category || !Array.isArray(category.children)) {
    return []
  }
  return [...category.children].sort(compareCategory)
}

export function findCategory(tree: CategoryTreeVO[], id: string): CategoryTreeVO | null {
  for (const item of tree) {
    if (item.id === id) {
      return item
    }
    const nested = findCategory(item.children || [], id)
    if (nested) {
      return nested
    }
  }
  return null
}

export function childSummary(category: CategoryTreeVO): string {
  return listChildren(category)
    .map((item) => item.name)
    .filter((name) => name.trim() !== '')
    .join(' / ')
}

export function buildHomeEntries(tree: CategoryTreeVO[]): HomeEntry[] {
  return listRootCategories(tree).map((category) => ({
    id: category.id,
    name: category.name,
    summary: childSummary(category),
    url: categoryPageUrl(category.id)
  }))
}

export function categoryPageUrl(id: string): string {
  return `/pages/category/category?id=${encodeURIComponent(id)}`
}

export function contentListUrl(categoryId: string): string {
  return `/pages/content-list/content-list?categoryId=${encodeURIComponent(categoryId)}`
}

export function resolveCategoryPage(input: {
  rawId: string
  detailName: string | null
  tree: CategoryTreeVO[] | null
  failure: RequestFailure | null
}): CategoryPageModel {
  const rawId = input.rawId.trim()
  if (!rawId) {
    return { status: 'empty', message: '请从首页选择分类', name: '', children: [] }
  }
  if (!isCategoryId(rawId)) {
    return { status: 'error', message: '分类不存在', name: '', children: [] }
  }
  if (input.failure) {
    const notFound = input.failure.code === '404' || input.failure.httpStatus === 404
    return {
      status: 'error',
      message: notFound ? '分类不存在' : input.failure.message,
      name: '',
      children: []
    }
  }
  if (!input.detailName || !input.tree) {
    return { status: 'error', message: '分类加载失败，请稍后重试', name: '', children: [] }
  }
  const children = listChildren(findCategory(input.tree, rawId)).map((item) => ({
    id: item.id,
    name: item.name,
    url: contentListUrl(item.id)
  }))
  if (children.length === 0) {
    return { status: 'empty', message: '暂无子分类', name: input.detailName, children: [] }
  }
  return { status: 'success', message: '', name: input.detailName, children }
}

function compareCategory(left: CategoryTreeVO, right: CategoryTreeVO): number {
  const sortDiff = left.sort - right.sort
  if (sortDiff !== 0) {
    return sortDiff
  }
  return left.id < right.id ? -1 : left.id > right.id ? 1 : 0
}

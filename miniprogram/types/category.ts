export type CategoryStatus = 'ENABLED' | 'DISABLED'

/**
 * 与 CategoryVO 一致。
 * id、parentId 是雪花 Long，后端序列化为字符串。根分类 parentId 为 "0"。
 */
export interface CategoryVO {
  id: string
  parentId: string
  name: string
  code: string
  iconUrl: string | null
  description: string | null
  sort: number
  status: CategoryStatus
}

/** 与 CategoryTreeVO 一致。children 由后端组装，没有下级时为空数组。 */
export interface CategoryTreeVO extends CategoryVO {
  children: CategoryTreeVO[]
}

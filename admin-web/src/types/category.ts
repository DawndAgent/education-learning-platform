export type CategoryStatus = 'ENABLED' | 'DISABLED'

export interface Category {
  id: string
  parentId: string
  name: string
  code: string
  iconUrl: string | null
  description: string | null
  sort: number
  status: CategoryStatus
  children?: Category[]
}

export interface CategoryPayload {
  parentId: string
  name: string
  code: string
  iconUrl: string
  description: string
  sort: number
  status: CategoryStatus
}

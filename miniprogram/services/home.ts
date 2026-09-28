import { request } from './request'
import type { HomePage } from '../types/home'

export function getHome(): Promise<HomePage> {
  return request.get<HomePage>('/api/home')
}

import type { ApiResponse } from '@/types/api'
import type { PageResult } from '@/types/content'
import type {
  BannerQuery,
  BannerSaveRequest,
  HomeBanner,
  HomeRecommendation,
  HomeSortItem,
  PublicHome,
  RecommendationCreateRequest,
  RecommendationQuery,
} from '@/types/home'
import request from './request'

export function getBanners(query: BannerQuery) {
  return request.get<ApiResponse<PageResult<HomeBanner>>>('/admin/api/home/banners', {
    params: compact({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword,
      status: query.status,
    }),
  })
}

export function createBanner(payload: BannerSaveRequest) {
  return request.post<ApiResponse<HomeBanner>>('/admin/api/home/banners', payload)
}

export function updateBanner(id: string, payload: BannerSaveRequest) {
  return request.put<ApiResponse<HomeBanner>>(`/admin/api/home/banners/${id}`, payload)
}

export function deleteBanner(id: string) {
  return request.delete<ApiResponse<null>>(`/admin/api/home/banners/${id}`)
}

export function enableBanner(id: string) {
  return request.post<ApiResponse<HomeBanner>>(`/admin/api/home/banners/${id}/enable`)
}

export function disableBanner(id: string) {
  return request.post<ApiResponse<HomeBanner>>(`/admin/api/home/banners/${id}/disable`)
}

export function sortBanners(items: HomeSortItem[]) {
  return request.put<ApiResponse<null>>('/admin/api/home/banners/sort', { items })
}

export function getRecommendations(query: RecommendationQuery) {
  return request.get<ApiResponse<PageResult<HomeRecommendation>>>('/admin/api/home/recommendations', {
    params: compact({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      recommendType: query.recommendType,
      status: query.status,
    }),
  })
}

export function createRecommendation(payload: RecommendationCreateRequest) {
  return request.post<ApiResponse<HomeRecommendation>>('/admin/api/home/recommendations', payload)
}

export function deleteRecommendation(id: string) {
  return request.delete<ApiResponse<null>>(`/admin/api/home/recommendations/${id}`)
}

export function sortRecommendations(items: HomeSortItem[]) {
  return request.put<ApiResponse<null>>('/admin/api/home/recommendations/sort', { items })
}

export function getPublicHome() {
  return request.get<ApiResponse<PublicHome>>('/api/home')
}

function compact(params: Record<string, string | number>) {
  const result: Record<string, string | number> = {}
  for (const [key, value] of Object.entries(params)) {
    if (value === '' || value === undefined || value === null) {
      continue
    }
    result[key] = value
  }
  return result
}

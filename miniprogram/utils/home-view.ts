import type { HomeEntry } from './category-tree'
import { categoryPageUrl } from './category-tree'
import { contentDetailUrl, contentTypeLabel, formatPublishDate, type LatestItem } from './content-view'
import { resolveMediaUrl } from './media-url'
import { topicDetailUrl } from './topic-view'
import type { HomeBanner, HomeCategory, HomeLatest, HomeRecommendation } from '../types/home'

export interface BannerCard {
  id: string
  title: string
  subtitle: string
  imageUrl: string
  action: 'content' | 'topic' | 'url' | 'none'
  url: string
  linkUrl: string
}

export interface RecommendCard {
  id: string
  title: string
  coverUrl: string
  typeLabel: string
  summary: string
  url: string
}

export interface NavTab {
  key: string
  kind: 'home' | 'category'
  name: string
  shortName: string
  iconText: string
  iconUrl: string
  categoryId: string
}

export function toBannerCards(banners: HomeBanner[]): BannerCard[] {
  return (banners || []).map((item) => {
    if (item.linkType === 'CONTENT' && item.linkId) {
      return card(item, 'content', contentDetailUrl(item.linkId), '')
    }
    if (item.linkType === 'TOPIC' && item.linkId) {
      return card(item, 'topic', topicDetailUrl(item.linkId), '')
    }
    if (item.linkType === 'URL' && item.linkUrl) {
      return card(item, 'url', '', item.linkUrl)
    }
    return card(item, 'none', '', '')
  })
}

export function toCategoryEntries(categories: HomeCategory[]): HomeEntry[] {
  return (categories || []).map((item) => ({
    id: item.id,
    name: item.name,
    summary: '',
    url: categoryPageUrl(item.id)
  }))
}

export function toNavTabs(categories: HomeCategory[]): NavTab[] {
  const home: NavTab = {
    key: 'home',
    kind: 'home',
    name: '首页',
    shortName: '首页',
    iconText: '首',
    iconUrl: '',
    categoryId: ''
  }
  const categoryTabs = (categories || []).map((item) => {
    const name = (item.name || '').trim() || '栏目'
    return {
      key: item.id,
      kind: 'category' as const,
      name,
      shortName: shortNavName(name),
      iconText: name.slice(0, 1),
      iconUrl: resolveMediaUrl(item.iconUrl || ''),
      categoryId: item.id
    }
  })
  return [home, ...categoryTabs]
}

export function shortNavName(name: string): string {
  const trimmed = name.trim()
  if (trimmed.length <= 4) {
    return trimmed
  }
  return trimmed.slice(0, 4)
}

export function toContentRecommendations(items: HomeRecommendation[]): RecommendCard[] {
  return (items || [])
    .filter((item) => item.type === 'CONTENT' && item.targetId)
    .map((item) => ({
      id: item.id,
      title: item.title,
      coverUrl: resolveMediaUrl(item.coverUrl || ''),
      typeLabel: contentTypeLabel(item.contentType || ''),
      summary: item.summary || '',
      url: contentDetailUrl(item.targetId)
    }))
}

export function toTopicRecommendations(items: HomeRecommendation[]): RecommendCard[] {
  return (items || [])
    .filter((item) => item.targetId)
    .map((item) => ({
      id: item.id,
      title: item.title,
      coverUrl: resolveMediaUrl(item.coverUrl || ''),
      typeLabel: '专题',
      summary: item.summary || '',
      url: topicDetailUrl(item.targetId)
    }))
}

export function toLatestCards(items: HomeLatest[]): LatestItem[] {
  return (items || []).map((item) => ({
    id: item.id,
    title: item.title,
    coverUrl: resolveMediaUrl(item.coverUrl || ''),
    typeLabel: contentTypeLabel(item.contentType),
    meta: '',
    summary: item.summary || '',
    dateText: formatPublishDate(item.publishTime),
    url: contentDetailUrl(item.id)
  }))
}

function card(item: HomeBanner, action: BannerCard['action'], url: string, linkUrl: string): BannerCard {
  return {
    id: item.id,
    title: item.title,
    subtitle: item.subtitle || '',
    imageUrl: resolveMediaUrl(item.imageUrl || ''),
    action,
    url,
    linkUrl
  }
}

import type { HomeEntry } from './category-tree'
import { categoryPageUrl } from './category-tree'
import { contentDetailUrl, contentTypeLabel, formatPublishDate, type LatestItem } from './content-view'
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

export function toContentRecommendations(items: HomeRecommendation[]): RecommendCard[] {
  return (items || [])
    .filter((item) => item.type === 'CONTENT' && item.targetId)
    .map((item) => ({
      id: item.id,
      title: item.title,
      coverUrl: item.coverUrl || '',
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
      coverUrl: item.coverUrl || '',
      typeLabel: '专题',
      summary: item.summary || '',
      url: topicDetailUrl(item.targetId)
    }))
}

export function toLatestCards(items: HomeLatest[]): LatestItem[] {
  return (items || []).map((item) => ({
    id: item.id,
    title: item.title,
    coverUrl: item.coverUrl || '',
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
    imageUrl: item.imageUrl || '',
    action,
    url,
    linkUrl
  }
}

package com.xxedu.learning.modules.home.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxedu.learning.modules.category.service.CategoryService;
import com.xxedu.learning.modules.category.vo.CategoryTreeVO;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.modules.home.banner.entity.HomeBanner;
import com.xxedu.learning.modules.home.banner.mapper.HomeBannerMapper;
import com.xxedu.learning.modules.home.enums.HomeItemStatus;
import com.xxedu.learning.modules.home.enums.HomeLinkType;
import com.xxedu.learning.modules.home.enums.RecommendType;
import com.xxedu.learning.modules.home.recommendation.entity.HomeRecommendation;
import com.xxedu.learning.modules.home.recommendation.mapper.HomeRecommendationMapper;
import com.xxedu.learning.modules.home.vo.HomePageVO;
import com.xxedu.learning.modules.topic.entity.Topic;
import com.xxedu.learning.modules.topic.enums.TopicStatus;
import com.xxedu.learning.modules.topic.mapper.TopicMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomeQueryService {

    public static final int BANNER_LIMIT = 5;
    public static final int RECOMMENDATION_LIMIT = 6;
    public static final int LATEST_LIMIT = 10;
    private static final int CANDIDATE_LIMIT = 50;

    private final HomeBannerMapper bannerMapper;
    private final HomeRecommendationMapper recommendationMapper;
    private final ContentMapper contentMapper;
    private final TopicMapper topicMapper;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public HomePageVO load(LocalDateTime now) {
        HomePageVO page = new HomePageVO();
        page.setBanners(loadBanners(now));
        page.setCategories(loadCategories());
        List<HomePageVO.Recommendation> recommendations = loadRecommendations();
        page.setRecommendations(recommendations);
        page.setTopics(recommendations.stream()
                .filter(item -> item.getType() == RecommendType.TOPIC)
                .toList());
        page.setLatestContents(loadLatest());
        return page;
    }

    private List<HomePageVO.Banner> loadBanners(LocalDateTime now) {
        List<HomeBanner> rows = bannerMapper.selectList(Wrappers.<HomeBanner>lambdaQuery()
                .select(HomeBanner::getId, HomeBanner::getTitle, HomeBanner::getSubtitle,
                        HomeBanner::getImageUrl, HomeBanner::getLinkType, HomeBanner::getLinkId, HomeBanner::getLinkUrl)
                .eq(HomeBanner::getStatus, HomeItemStatus.ENABLED)
                .and(wrapper -> wrapper.isNull(HomeBanner::getStartTime).or().le(HomeBanner::getStartTime, now))
                .and(wrapper -> wrapper.isNull(HomeBanner::getEndTime).or().ge(HomeBanner::getEndTime, now))
                .orderByAsc(HomeBanner::getSort)
                .orderByDesc(HomeBanner::getId)
                .last("LIMIT " + CANDIDATE_LIMIT));
        Map<Long, Content> contents = contentsById(rows.stream()
                .filter(item -> item.getLinkType() == HomeLinkType.CONTENT && item.getLinkId() != null)
                .map(HomeBanner::getLinkId).distinct().toList());
        Map<Long, Topic> topics = topicsById(rows.stream()
                .filter(item -> item.getLinkType() == HomeLinkType.TOPIC && item.getLinkId() != null)
                .map(HomeBanner::getLinkId).distinct().toList());
        List<HomePageVO.Banner> banners = new ArrayList<>();
        for (HomeBanner row : rows) {
            if (!bannerVisible(row, contents, topics)) {
                continue;
            }
            HomePageVO.Banner banner = new HomePageVO.Banner();
            banner.setId(row.getId());
            banner.setTitle(row.getTitle());
            banner.setSubtitle(row.getSubtitle());
            banner.setImageUrl(row.getImageUrl());
            banner.setLinkType(row.getLinkType());
            banner.setLinkId(row.getLinkType() == HomeLinkType.URL || row.getLinkType() == HomeLinkType.NONE
                    ? null : row.getLinkId());
            banner.setLinkUrl(row.getLinkType() == HomeLinkType.URL ? row.getLinkUrl() : null);
            banners.add(banner);
            if (banners.size() == BANNER_LIMIT) {
                break;
            }
        }
        return banners;
    }

    private boolean bannerVisible(HomeBanner row, Map<Long, Content> contents, Map<Long, Topic> topics) {
        if (row.getLinkType() == HomeLinkType.CONTENT) {
            Content content = contents.get(row.getLinkId());
            return content != null && content.getStatus() == ContentStatus.PUBLISHED;
        }
        if (row.getLinkType() == HomeLinkType.TOPIC) {
            Topic topic = topics.get(row.getLinkId());
            return topic != null && topic.getStatus() == TopicStatus.PUBLISHED;
        }
        return true;
    }

    private List<HomePageVO.Category> loadCategories() {
        List<HomePageVO.Category> categories = new ArrayList<>();
        for (CategoryTreeVO root : categoryService.publicTree()) {
            HomePageVO.Category item = new HomePageVO.Category();
            item.setId(root.getId());
            item.setName(root.getName());
            item.setCode(root.getCode());
            item.setIconUrl(root.getIconUrl());
            categories.add(item);
        }
        return categories;
    }

    private List<HomePageVO.Recommendation> loadRecommendations() {
        List<HomeRecommendation> rows = recommendationMapper.selectList(Wrappers.<HomeRecommendation>lambdaQuery()
                .select(HomeRecommendation::getId, HomeRecommendation::getRecommendType, HomeRecommendation::getTargetId)
                .eq(HomeRecommendation::getStatus, HomeItemStatus.ENABLED)
                .orderByAsc(HomeRecommendation::getSort)
                .orderByDesc(HomeRecommendation::getId)
                .last("LIMIT " + CANDIDATE_LIMIT));
        Map<Long, Content> contents = contentsById(rows.stream()
                .filter(item -> item.getRecommendType() == RecommendType.CONTENT)
                .map(HomeRecommendation::getTargetId).distinct().toList());
        Map<Long, Topic> topics = topicsById(rows.stream()
                .filter(item -> item.getRecommendType() == RecommendType.TOPIC)
                .map(HomeRecommendation::getTargetId).distinct().toList());
        List<HomePageVO.Recommendation> result = new ArrayList<>();
        for (HomeRecommendation row : rows) {
            HomePageVO.Recommendation item = toRecommendation(row, contents, topics);
            if (item == null) {
                continue;
            }
            result.add(item);
            if (result.size() == RECOMMENDATION_LIMIT) {
                break;
            }
        }
        return result;
    }

    private HomePageVO.Recommendation toRecommendation(
            HomeRecommendation row, Map<Long, Content> contents, Map<Long, Topic> topics) {
        HomePageVO.Recommendation item = new HomePageVO.Recommendation();
        item.setId(row.getId());
        item.setType(row.getRecommendType());
        item.setTargetId(row.getTargetId());
        if (row.getRecommendType() == RecommendType.CONTENT) {
            Content content = contents.get(row.getTargetId());
            if (content == null || content.getStatus() != ContentStatus.PUBLISHED) {
                return null;
            }
            item.setTitle(content.getTitle());
            item.setCoverUrl(content.getCoverUrl());
            item.setSummary(content.getSummary());
            item.setContentType(content.getContentType());
            return item;
        }
        Topic topic = topics.get(row.getTargetId());
        if (topic == null || topic.getStatus() != TopicStatus.PUBLISHED) {
            return null;
        }
        item.setTitle(topic.getName());
        item.setCoverUrl(topic.getCoverUrl());
        item.setSummary(topic.getSummary());
        return item;
    }

    private List<HomePageVO.Latest> loadLatest() {
        List<Content> rows = contentMapper.selectList(Wrappers.<Content>lambdaQuery()
                .select(Content::getId, Content::getTitle, Content::getContentType,
                        Content::getCoverUrl, Content::getSummary, Content::getPublishTime)
                .eq(Content::getStatus, ContentStatus.PUBLISHED)
                .orderByDesc(Content::getPublishTime)
                .orderByDesc(Content::getId)
                .last("LIMIT " + LATEST_LIMIT));
        List<HomePageVO.Latest> latest = new ArrayList<>(rows.size());
        for (Content row : rows) {
            HomePageVO.Latest item = new HomePageVO.Latest();
            item.setId(row.getId());
            item.setTitle(row.getTitle());
            item.setContentType(row.getContentType());
            item.setCoverUrl(row.getCoverUrl());
            item.setSummary(row.getSummary());
            item.setPublishTime(row.getPublishTime());
            latest.add(item);
        }
        return latest;
    }

    private Map<Long, Content> contentsById(List<Long> ids) {
        Map<Long, Content> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        contentMapper.selectList(Wrappers.<Content>lambdaQuery()
                        .select(Content::getId, Content::getTitle, Content::getCoverUrl, Content::getSummary,
                                Content::getContentType, Content::getStatus)
                        .in(Content::getId, ids))
                .forEach(item -> map.put(item.getId(), item));
        return map;
    }

    private Map<Long, Topic> topicsById(List<Long> ids) {
        Map<Long, Topic> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        topicMapper.selectList(Wrappers.<Topic>lambdaQuery()
                        .select(Topic::getId, Topic::getName, Topic::getCoverUrl, Topic::getSummary, Topic::getStatus)
                        .in(Topic::getId, ids))
                .forEach(item -> map.put(item.getId(), item));
        return map;
    }
}

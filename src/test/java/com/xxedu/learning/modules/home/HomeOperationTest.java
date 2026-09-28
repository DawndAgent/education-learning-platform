package com.xxedu.learning.modules.home;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.home.banner.dto.BannerSaveRequest;
import com.xxedu.learning.modules.home.banner.service.HomeBannerService;
import com.xxedu.learning.modules.home.banner.vo.BannerAdminVO;
import com.xxedu.learning.modules.home.dto.HomeSortRequest;
import com.xxedu.learning.modules.home.enums.HomeItemStatus;
import com.xxedu.learning.modules.home.enums.HomeLinkType;
import com.xxedu.learning.modules.home.enums.RecommendType;
import com.xxedu.learning.modules.home.recommendation.dto.RecommendationCreateRequest;
import com.xxedu.learning.modules.home.recommendation.service.HomeRecommendationService;
import com.xxedu.learning.modules.home.service.HomeQueryService;
import com.xxedu.learning.modules.home.vo.HomePageVO;
import com.xxedu.learning.modules.topic.dto.TopicContentAddRequest;
import com.xxedu.learning.modules.topic.dto.TopicCreateRequest;
import com.xxedu.learning.modules.topic.service.TopicService;
import com.xxedu.learning.modules.topic.vo.TopicDetailVO;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class HomeOperationTest extends IntegrationTestSupport {

    @Autowired
    private HomeBannerService homeBannerService;

    @Autowired
    private HomeRecommendationService homeRecommendationService;

    @Autowired
    private HomeQueryService homeQueryService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private TopicService topicService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void bannerLifecycleAndHomeFilters() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 12, 0);
        BannerAdminVO active = homeBannerService.create(banner("有效", HomeLinkType.NONE, null, null, 1, null, null));
        BannerAdminVO disabled = homeBannerService.create(banner("停用", HomeLinkType.NONE, null, null, 2, null, null));
        BannerAdminVO future = homeBannerService.create(banner("未开始", HomeLinkType.NONE, null, null, 3,
                now.plusDays(1), null));
        BannerAdminVO expired = homeBannerService.create(banner("已过期", HomeLinkType.NONE, null, null, 4,
                null, now.minusDays(1)));
        homeBannerService.enable(active.getId());
        homeBannerService.enable(disabled.getId());
        homeBannerService.disable(disabled.getId());
        homeBannerService.enable(future.getId());
        homeBannerService.enable(expired.getId());

        HomePageVO page = homeQueryService.load(now);
        assertThat(page.getBanners()).extracting(HomePageVO.Banner::getTitle).containsExactly("有效");

        homeBannerService.enable(disabled.getId());
        assertThat(homeQueryService.load(now).getBanners()).extracting(HomePageVO.Banner::getTitle)
                .contains("有效", "停用");

        homeBannerService.delete(active.getId());
        assertThat(homeQueryService.load(now).getBanners()).extracting(HomePageVO.Banner::getTitle)
                .doesNotContain("有效");
    }

    @Test
    void bannerSortAndLimit() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 12, 0);
        BannerAdminVO third = enable(banner("三", 3));
        BannerAdminVO first = enable(banner("一", 1));
        BannerAdminVO second = enable(banner("二", 2));
        for (int i = 4; i <= 7; i++) {
            enable(banner("额外" + i, i));
        }
        HomeSortRequest sort = new HomeSortRequest();
        HomeSortRequest.Item a = new HomeSortRequest.Item();
        a.setId(first.getId());
        a.setSort(1);
        HomeSortRequest.Item b = new HomeSortRequest.Item();
        b.setId(second.getId());
        b.setSort(2);
        HomeSortRequest.Item c = new HomeSortRequest.Item();
        c.setId(third.getId());
        c.setSort(3);
        sort.setItems(List.of(a, b, c));
        homeBannerService.sort(sort);

        List<String> titles = homeQueryService.load(now).getBanners().stream().map(HomePageVO.Banner::getTitle).toList();
        assertThat(titles).hasSize(HomeQueryService.BANNER_LIMIT);
        assertThat(titles.get(0)).isEqualTo("一");
        assertThat(titles.get(1)).isEqualTo("二");
        assertThat(titles.get(2)).isEqualTo("三");
    }

    @Test
    void recommendationHidesOfflineAndRestoresAfterRepublish() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 12, 0);
        Long publishedId = publishArticle("已发布推荐");
        Long offlineId = publishArticle("下线推荐");
        contentService.offline(offlineId);
        TopicDetailVO topic = publishTopic("首页专题", "HOME_TOPIC_10", publishedId);
        TopicDetailVO offlineTopic = publishTopic("下线专题", "HOME_TOPIC_OFF", publishedId);
        topicService.offline(offlineTopic.getId());

        homeRecommendationService.create(recommend(RecommendType.CONTENT, publishedId, 1));
        homeRecommendationService.create(recommend(RecommendType.CONTENT, offlineId, 2));
        homeRecommendationService.create(recommend(RecommendType.TOPIC, topic.getId(), 3));
        homeRecommendationService.create(recommend(RecommendType.TOPIC, offlineTopic.getId(), 4));
        assertThatThrownBy(() -> homeRecommendationService.create(recommend(RecommendType.CONTENT, publishedId, 9)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该目标已在重点推荐中");

        HomePageVO hidden = homeQueryService.load(now);
        assertThat(hidden.getRecommendations()).extracting(HomePageVO.Recommendation::getTargetId)
                .containsExactly(publishedId, topic.getId());
        assertThat(hidden.getTopics()).extracting(HomePageVO.Recommendation::getTargetId)
                .containsExactly(topic.getId());

        contentService.publish(offlineId);
        topicService.publish(offlineTopic.getId());
        assertThat(homeQueryService.load(now).getRecommendations()).extracting(HomePageVO.Recommendation::getTargetId)
                .contains(publishedId, offlineId, topic.getId(), offlineTopic.getId());
    }

    @Test
    void recommendationLimit() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 12, 0);
        for (int i = 0; i < 8; i++) {
            Long id = publishArticle("推荐" + i);
            homeRecommendationService.create(recommend(RecommendType.CONTENT, id, i));
        }
        assertThat(homeQueryService.load(now).getRecommendations()).hasSize(HomeQueryService.RECOMMENDATION_LIMIT);
        assertThat(homeQueryService.load(now).getCategories()).isNotEmpty();
        assertThat(homeQueryService.load(now).getLatestContents()).isNotEmpty();
    }

    @Test
    void recommendationCanBeRemovedAndAddedAgain() {
        Long contentId = publishArticle("反复推荐");
        var created = homeRecommendationService.create(recommend(RecommendType.CONTENT, contentId, 1));
        homeRecommendationService.delete(created.getId());
        var again = homeRecommendationService.create(recommend(RecommendType.CONTENT, contentId, 2));
        homeRecommendationService.delete(again.getId());
        var third = homeRecommendationService.create(recommend(RecommendType.CONTENT, contentId, 3));
        assertThat(homeQueryService.load(LocalDateTime.of(2026, 10, 3, 12, 0)).getRecommendations())
                .extracting(HomePageVO.Recommendation::getTargetId)
                .contains(contentId);
        homeRecommendationService.delete(third.getId());
    }

    private BannerAdminVO enable(BannerSaveRequest request) {
        BannerAdminVO created = homeBannerService.create(request);
        return homeBannerService.enable(created.getId());
    }

    private BannerSaveRequest banner(String title, int sort) {
        return banner(title, HomeLinkType.NONE, null, null, sort, null, null);
    }

    private BannerSaveRequest banner(String title, HomeLinkType type, Long linkId, String linkUrl, int sort,
                                     LocalDateTime start, LocalDateTime end) {
        BannerSaveRequest request = new BannerSaveRequest();
        request.setTitle(title);
        request.setSubtitle("副标题");
        request.setImageUrl("https://example.com/banner.png");
        request.setLinkType(type);
        request.setLinkId(linkId);
        request.setLinkUrl(linkUrl);
        request.setSort(sort);
        request.setStartTime(start);
        request.setEndTime(end);
        return request;
    }

    private RecommendationCreateRequest recommend(RecommendType type, Long targetId, int sort) {
        RecommendationCreateRequest request = new RecommendationCreateRequest();
        request.setRecommendType(type);
        request.setTargetId(targetId);
        request.setSort(sort);
        return request;
    }

    private Long publishArticle(String title) {
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setSort(1);
        request.setBody("正文");
        Long id = articleService.create(request).getContentId();
        assertThat(contentService.publish(id).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        return id;
    }

    private TopicDetailVO publishTopic(String name, String code, Long contentId) {
        TopicCreateRequest request = new TopicCreateRequest();
        request.setName(name);
        request.setCode(code);
        request.setCategoryId(11L);
        request.setSort(1);
        request.setSummary("简介");
        TopicDetailVO topic = topicService.create(request);
        TopicContentAddRequest add = new TopicContentAddRequest();
        add.setContentIds(List.of(contentId));
        topicService.addContents(topic.getId(), add);
        return topicService.publish(topic.getId());
    }
}

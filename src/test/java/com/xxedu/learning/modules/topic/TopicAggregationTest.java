package com.xxedu.learning.modules.topic;

import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.content.dto.SchedulePublishRequest;
import com.xxedu.learning.modules.content.service.ContentPublishJob;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.home.enums.RecommendType;
import com.xxedu.learning.modules.home.recommendation.dto.RecommendationCreateRequest;
import com.xxedu.learning.modules.home.recommendation.service.HomeRecommendationService;
import com.xxedu.learning.modules.home.service.HomeQueryService;
import com.xxedu.learning.modules.home.vo.HomePageVO;
import com.xxedu.learning.modules.topic.dto.TopicContentAddRequest;
import com.xxedu.learning.modules.topic.dto.TopicCreateRequest;
import com.xxedu.learning.modules.topic.dto.TopicItemAddRequest;
import com.xxedu.learning.modules.topic.dto.TopicItemBatchRequest;
import com.xxedu.learning.modules.topic.dto.TopicItemSortRequest;
import com.xxedu.learning.modules.topic.service.TopicService;
import com.xxedu.learning.modules.topic.vo.PublicTopicContentVO;
import com.xxedu.learning.modules.topic.vo.TopicContentItemVO;
import com.xxedu.learning.modules.topic.vo.TopicDetailVO;
import com.xxedu.learning.modules.topic.vo.TopicItemBatchResultVO;
import com.xxedu.learning.modules.video.dto.VideoCreateRequest;
import com.xxedu.learning.modules.video.enums.VideoSourceType;
import com.xxedu.learning.modules.video.service.VideoService;
import com.xxedu.learning.modules.weekly.dto.WeeklyCreateRequest;
import com.xxedu.learning.modules.weekly.service.WeeklyService;
import com.xxedu.learning.security.JwtTokenService;
import com.xxedu.learning.security.LoginUser;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class TopicAggregationTest extends IntegrationTestSupport {

    private static final DateTimeFormatter WALL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TopicService topicService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private WeeklyService weeklyService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private ContentPublishJob contentPublishJob;

    @Autowired
    private HomeRecommendationService homeRecommendationService;

    @Autowired
    private HomeQueryService homeQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void articleAndVideoStayLinkedAcrossOfflineAndRepublish() {
        TopicDetailVO topic = topicService.create(topic("聚合专题", "AGG_TOPIC_11"));
        Long articleId = articleService.create(article("聚合文章")).getContentId();
        Long videoId = videoService.create(video("聚合视频")).getContentId();
        contentService.publish(articleId);
        contentService.publish(videoId);
        TopicContentAddRequest add = new TopicContentAddRequest();
        add.setContentIds(List.of(articleId, videoId));
        topicService.addContents(topic.getId(), add);
        topicService.publish(topic.getId());

        assertThat(topicService.publicDetail(topic.getId()).getContents())
                .extracting(PublicTopicContentVO::getTitle)
                .containsExactly("聚合文章", "聚合视频");

        contentService.offline(videoId);
        assertThat(topicService.publicDetail(topic.getId()).getContents())
                .extracting(PublicTopicContentVO::getTitle)
                .containsExactly("聚合文章");

        contentService.publish(videoId);
        assertThat(topicService.publicDetail(topic.getId()).getContents())
                .extracting(PublicTopicContentVO::getId)
                .containsExactly(articleId, videoId);
    }

    @Test
    void scheduledDraftAppearsAfterPublishJob() {
        TopicDetailVO topic = topicService.create(topic("定时专题", "AGG_SCHEDULE_11"));
        Long readyId = articleService.create(article("已发布锚点")).getContentId();
        Long pendingId = articleService.create(article("明天的文章")).getContentId();
        contentService.publish(readyId);
        SchedulePublishRequest schedule = new SchedulePublishRequest();
        schedule.setPublishTime(LocalDateTime.now().plusDays(1).format(WALL));
        contentService.schedulePublish(pendingId, schedule);
        TopicContentAddRequest add = new TopicContentAddRequest();
        add.setContentIds(List.of(readyId, pendingId));
        topicService.addContents(topic.getId(), add);
        topicService.publish(topic.getId());

        assertThat(topicService.publicDetail(topic.getId()).getContents())
                .extracting(PublicTopicContentVO::getTitle)
                .containsExactly("已发布锚点");

        jdbcTemplate.update(
                "UPDATE content SET scheduled_publish_time = ? WHERE id = ?",
                LocalDateTime.now().minusMinutes(1), pendingId);
        contentPublishJob.runOnce();

        assertThat(topicService.publicDetail(topic.getId()).getContents())
                .extracting(PublicTopicContentVO::getTitle)
                .containsExactly("已发布锚点", "明天的文章");
    }

    @Test
    void batchAddCountsDuplicatesAndInvalidWithoutFailing() {
        TopicDetailVO topic = topicService.create(topic("批量专题", "AGG_BATCH_11"));
        Long articleId = articleService.create(article("批量文章")).getContentId();
        TopicItemAddRequest first = new TopicItemAddRequest();
        first.setContentId(articleId);
        first.setSort(3);
        TopicContentItemVO created = topicService.addItem(topic.getId(), first);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getSort()).isEqualTo(3);

        TopicItemBatchRequest batch = new TopicItemBatchRequest();
        batch.setContentIds(List.of(articleId, articleId, 9_999_999_999L));
        TopicItemBatchResultVO result = topicService.addItems(topic.getId(), batch);
        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getDuplicateCount()).isEqualTo(1);
        assertThat(result.getInvalidCount()).isEqualTo(1);
        assertThat(topicService.listContents(topic.getId())).hasSize(1);
    }

    @Test
    void removeItemDoesNotDeleteContentAndSortRejectsOtherTopic() {
        TopicDetailVO topic = topicService.create(topic("条目专题", "AGG_ITEM_11"));
        TopicDetailVO other = topicService.create(topic("另一个专题", "AGG_ITEM_OTHER"));
        Long articleId = articleService.create(article("保留文章")).getContentId();
        Long otherArticleId = articleService.create(article("别的文章")).getContentId();
        TopicItemAddRequest add = new TopicItemAddRequest();
        add.setContentId(articleId);
        TopicContentItemVO item = topicService.addItem(topic.getId(), add);
        TopicItemAddRequest otherAdd = new TopicItemAddRequest();
        otherAdd.setContentId(otherArticleId);
        TopicContentItemVO otherItem = topicService.addItem(other.getId(), otherAdd);

        TopicItemSortRequest sort = new TopicItemSortRequest();
        TopicItemSortRequest.Item foreign = new TopicItemSortRequest.Item();
        foreign.setItemId(otherItem.getId());
        foreign.setSort(1);
        sort.setItems(List.of(foreign));
        assertThatThrownBy(() -> topicService.sortItems(topic.getId(), sort))
                .isInstanceOf(BusinessException.class)
                .hasMessage("专题内容不属于当前专题");

        topicService.removeItem(topic.getId(), item.getId());
        assertThat(topicService.listContents(topic.getId())).isEmpty();
        assertThat(articleService.adminDetail(articleId).getTitle()).isEqualTo("保留文章");
        Integer relation = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM topic_content WHERE id = ? AND deleted = 1",
                Integer.class, item.getId());
        assertThat(relation).isEqualTo(1);
    }

    @Test
    void publishedWeeklyIsVisibleAndDeletedContentIsHidden() {
        TopicDetailVO topic = topicService.create(topic("类型专题", "AGG_TYPE_11"));
        Long weeklyId = weeklyService.create(weekly("每周专题题")).getContentId();
        contentService.publish(weeklyId);
        TopicContentAddRequest add = new TopicContentAddRequest();
        add.setContentIds(List.of(weeklyId));
        topicService.addContents(topic.getId(), add);
        topicService.publish(topic.getId());
        assertThat(topicService.publicDetail(topic.getId()).getContents())
                .extracting(PublicTopicContentVO::getContentType)
                .containsExactly(com.xxedu.learning.modules.content.enums.ContentType.WEEKLY);

        contentService.offline(weeklyId);
        contentService.delete(weeklyId);
        assertThat(topicService.publicDetail(topic.getId()).getContents()).isEmpty();
        assertThat(topicService.adminDetail(topic.getId()).getStatus())
                .isEqualTo(com.xxedu.learning.modules.topic.enums.TopicStatus.PUBLISHED);
    }

    @Test
    void homeRecommendationStillOpensPublishedTopic() {
        TopicDetailVO topic = topicService.create(topic("首页专题", "AGG_HOME_11"));
        Long articleId = articleService.create(article("首页文章")).getContentId();
        contentService.publish(articleId);
        TopicContentAddRequest add = new TopicContentAddRequest();
        add.setContentIds(List.of(articleId));
        topicService.addContents(topic.getId(), add);
        topicService.publish(topic.getId());
        RecommendationCreateRequest recommend = new RecommendationCreateRequest();
        recommend.setRecommendType(RecommendType.TOPIC);
        recommend.setTargetId(topic.getId());
        recommend.setSort(1);
        homeRecommendationService.create(recommend);

        assertThat(homeQueryService.load(LocalDateTime.now()).getTopics())
                .extracting(HomePageVO.Recommendation::getTargetId)
                .contains(topic.getId());
        assertThat(topicService.publicDetail(topic.getId()).getContents())
                .extracting(PublicTopicContentVO::getId)
                .containsExactly(articleId);
    }

    @Test
    void topicWritePermissionsAreEnforced() throws Exception {
        String body = """
                {"name":"无权限专题","code":"NO_PERM_11","categoryId":"11","sort":1}
                """;
        mockMvc.perform(post("/admin/api/topics")
                        .header("Authorization", "Bearer " + token(PermissionCodes.TOPIC_VIEW))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"));

        TestAuth.loginOperator();
        TopicDetailVO topic = topicService.create(topic("权限专题", "PERM_TOPIC_11"));
        mockMvc.perform(put("/admin/api/topics/" + topic.getId())
                        .header("Authorization", "Bearer " + token(PermissionCodes.TOPIC_VIEW))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/admin/api/topics/" + topic.getId())
                        .header("Authorization", "Bearer " + token(PermissionCodes.TOPIC_VIEW)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/api/topics/" + topic.getId() + "/publish")
                        .header("Authorization", "Bearer " + token(PermissionCodes.TOPIC_VIEW)))
                .andExpect(status().isForbidden());
    }

    private String token(String... permissions) {
        return jwtTokenService.issue(new LoginUser(8L, "admin", ClientType.ADMIN, Set.of(permissions)));
    }

    private TopicCreateRequest topic(String name, String code) {
        TopicCreateRequest request = new TopicCreateRequest();
        request.setName(name);
        request.setCode(code);
        request.setCategoryId(11L);
        request.setSort(1);
        request.setSummary("简介");
        return request;
    }

    private ArticleCreateRequest article(String title) {
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setSort(1);
        request.setBody("正文");
        return request;
    }

    private VideoCreateRequest video(String title) {
        VideoCreateRequest request = new VideoCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setSort(1);
        request.setSourceType(VideoSourceType.WECHAT_CHANNEL);
        request.setVideoUrl("https://channels.weixin.qq.com/example");
        request.setQrCodeUrl("https://example.com/qr.png");
        return request;
    }

    private WeeklyCreateRequest weekly(String title) {
        WeeklyCreateRequest request = new WeeklyCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setSort(1);
        request.setWeekLabel("2026-W11");
        request.setQuestionText("题干");
        request.setAnswerText("答案");
        return request;
    }
}

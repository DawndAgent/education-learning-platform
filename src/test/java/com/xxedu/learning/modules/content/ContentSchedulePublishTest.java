package com.xxedu.learning.modules.content;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.entity.Article;
import com.xxedu.learning.modules.article.mapper.ArticleMapper;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.article.vo.ArticleDetailVO;
import com.xxedu.learning.modules.content.dto.SchedulePublishRequest;
import com.xxedu.learning.modules.content.entity.Content;
import com.xxedu.learning.modules.content.enums.ContentScheduleFilter;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.mapper.ContentMapper;
import com.xxedu.learning.modules.content.service.ContentBatchPublishService;
import com.xxedu.learning.modules.content.service.ContentPublishJob;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.content.vo.ContentBatchResultVO;
import com.xxedu.learning.modules.content.vo.ContentDetailVO;
import com.xxedu.learning.modules.document.dto.DocumentCreateRequest;
import com.xxedu.learning.modules.document.service.DocumentService;
import com.xxedu.learning.modules.question.dto.QuestionCreateRequest;
import com.xxedu.learning.modules.question.enums.QuestionType;
import com.xxedu.learning.modules.question.service.QuestionService;
import com.xxedu.learning.modules.topic.dto.TopicContentAddRequest;
import com.xxedu.learning.modules.topic.dto.TopicCreateRequest;
import com.xxedu.learning.modules.topic.service.TopicService;
import com.xxedu.learning.modules.topic.vo.PublicTopicContentVO;
import com.xxedu.learning.modules.video.dto.VideoCreateRequest;
import com.xxedu.learning.modules.video.enums.VideoSourceType;
import com.xxedu.learning.modules.video.service.VideoService;
import com.xxedu.learning.modules.weekly.dto.WeeklyCreateRequest;
import com.xxedu.learning.modules.weekly.service.WeeklyService;
import com.xxedu.learning.security.PermissionCodes;
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
class ContentSchedulePublishTest extends IntegrationTestSupport {

    @Autowired
    private ContentService contentService;

    @Autowired
    private ContentPublishJob contentPublishJob;

    @Autowired
    private ContentBatchPublishService contentBatchPublishService;

    @Autowired
    private ContentMapper contentMapper;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private VideoService videoService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private WeeklyService weeklyService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private TopicService topicService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void scheduleStaysDraftUntilDueThenPublishes() {
        ArticleDetailVO article = articleService.create(article("定时文章"));
        LocalDateTime future = LocalDateTime.now().plusHours(2).withNano(0);
        ContentDetailVO scheduled = contentService.schedulePublish(article.getContentId(), schedule(future));

        assertThat(scheduled.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(scheduled.getScheduledPublishTime()).isEqualTo(future);
        assertThat(scheduled.getPublishTime()).isNull();
        assertThatThrownBy(() -> contentService.publicDetail(article.getContentId()))
                .isInstanceOf(BusinessException.class);

        markDue(article.getContentId());
        contentPublishJob.runOnce();

        ContentDetailVO published = contentService.adminDetail(article.getContentId());
        assertThat(published.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(published.getPublishTime()).isNotNull();
        assertThat(published.getScheduledPublishTime()).isNull();
        assertThat(contentService.publicDetail(article.getContentId()).getTitle()).isEqualTo("定时文章");
    }

    @Test
    void pastScheduleTimeIsRejected() {
        ArticleDetailVO article = articleService.create(article("过期时间"));
        assertThatThrownBy(() -> contentService.schedulePublish(
                article.getContentId(), schedule(LocalDateTime.now().minusMinutes(1))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("发布时间必须晚于当前时间");
    }

    @Test
    void cancelClearsScheduleAndKeepsDraft() {
        ArticleDetailVO article = articleService.create(article("取消定时"));
        contentService.schedulePublish(article.getContentId(), schedule(LocalDateTime.now().plusDays(1)));
        ContentDetailVO cancelled = contentService.cancelScheduledPublish(article.getContentId());
        assertThat(cancelled.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(cancelled.getScheduledPublishTime()).isNull();
    }

    @Test
    void immediatePublishClearsSchedule() {
        ArticleDetailVO article = articleService.create(article("立即覆盖"));
        contentService.schedulePublish(article.getContentId(), schedule(LocalDateTime.now().plusDays(1)));
        ContentDetailVO published = contentService.publish(article.getContentId());
        assertThat(published.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(published.getScheduledPublishTime()).isNull();
        assertThat(published.getPublishTime()).isNotNull();

        int raced = contentMapper.publishFromDraft(article.getContentId(), LocalDateTime.now());
        assertThat(raced).isZero();
        assertThat(contentService.adminDetail(article.getContentId()).getPublishTime())
                .isEqualTo(published.getPublishTime());
    }

    @Test
    void offlineClearsSchedule() {
        ArticleDetailVO article = articleService.create(article("下线清理"));
        contentService.publish(article.getContentId());
        ContentDetailVO offline = contentService.offline(article.getContentId());
        assertThat(offline.getStatus()).isEqualTo(ContentStatus.OFFLINE);
        assertThat(offline.getScheduledPublishTime()).isNull();
    }

    @Test
    void publishedContentCannotBeScheduled() {
        ArticleDetailVO article = articleService.create(article("已发布禁止定时"));
        contentService.publish(article.getContentId());
        assertThatThrownBy(() -> contentService.schedulePublish(
                article.getContentId(), schedule(LocalDateTime.now().plusHours(1))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("已发布内容不能定时发布");
    }

    @Test
    void offlineCanBeScheduledBackToDraft() {
        ArticleDetailVO article = articleService.create(article("下线后再定时"));
        contentService.publish(article.getContentId());
        contentService.offline(article.getContentId());
        ContentDetailVO scheduled = contentService.schedulePublish(
                article.getContentId(), schedule(LocalDateTime.now().plusHours(3).withNano(0)));
        assertThat(scheduled.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(scheduled.getScheduledPublishTime()).isNotNull();
    }

    @Test
    void eachWritableTypeCanBeScheduled() {
        Long articleId = articleService.create(article("类型文章")).getContentId();
        Long videoId = videoService.create(video("类型视频")).getContentId();
        Long questionId = questionService.create(question("类型题目")).getContentId();
        Long weeklyId = weeklyService.create(weekly("类型每周")).getContentId();
        Long documentId = documentService.create(document("类型资料")).getContentId();
        for (Long id : List.of(articleId, videoId, questionId, weeklyId, documentId)) {
            contentService.schedulePublish(id, schedule(LocalDateTime.now().plusHours(1)));
            markDue(id);
        }
        contentPublishJob.runOnce();
        for (Long id : List.of(articleId, videoId, questionId, weeklyId, documentId)) {
            ContentDetailVO detail = contentService.adminDetail(id);
            assertThat(detail.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
            assertThat(detail.getScheduledPublishTime()).isNull();
        }
    }

    @Test
    void failedItemDoesNotBlockTheBatch() {
        Long readyId = articleService.create(article("可发布")).getContentId();
        Long brokenId = articleService.create(article("校验失败")).getContentId();
        contentService.schedulePublish(readyId, schedule(LocalDateTime.now().plusHours(1)));
        contentService.schedulePublish(brokenId, schedule(LocalDateTime.now().plusHours(1)));
        articleMapper.update(null, Wrappers.<Article>lambdaUpdate()
                .set(Article::getBody, "")
                .eq(Article::getContentId, brokenId));
        markDue(readyId);
        markDue(brokenId);

        contentPublishJob.runOnce();

        assertThat(contentService.adminDetail(readyId).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(contentService.adminDetail(brokenId).getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(contentService.adminDetail(brokenId).getScheduledPublishTime()).isNotNull();
    }

    @Test
    void batchPublishReportsPartialFailure() {
        Long readyId = articleService.create(article("批量成功")).getContentId();
        Long blockedId = contentService.create(bare("批量失败")).getId();
        ContentBatchResultVO result = contentBatchPublishService.batchPublish(List.of(readyId, blockedId));
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(contentService.adminDetail(readyId).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(contentService.adminDetail(blockedId).getStatus()).isEqualTo(ContentStatus.DRAFT);
    }

    @Test
    void scheduleFilterAndPublishTimeRange() {
        ArticleDetailVO article = articleService.create(article("筛选文章"));
        contentService.schedulePublish(article.getContentId(), schedule(LocalDateTime.now().plusDays(2)));
        var query = new com.xxedu.learning.modules.content.dto.ContentQueryRequest();
        query.setPageNum(1);
        query.setPageSize(20);
        query.setSchedule(ContentScheduleFilter.SCHEDULED);
        assertThat(contentService.adminPage(query).getRecords())
                .extracting(item -> item.getId())
                .contains(article.getContentId());

        contentService.publish(article.getContentId());
        query.setSchedule(null);
        query.setStatus(ContentStatus.PUBLISHED);
        query.setPublishTimeFrom(LocalDateTime.now().minusMinutes(5));
        query.setPublishTimeTo(LocalDateTime.now().plusMinutes(5));
        assertThat(contentService.adminPage(query).getRecords())
                .extracting(item -> item.getId())
                .contains(article.getContentId());
    }

    @Test
    void topicShowsContentOnlyAfterScheduledPublish() {
        ArticleDetailVO anchor = articleService.create(article("专题锚点"));
        contentService.publish(anchor.getContentId());
        ArticleDetailVO pending = articleService.create(article("专题待发布"));
        var topic = topicService.create(topic("定时专题", "TOPIC_SCHEDULE_09"));
        TopicContentAddRequest add = new TopicContentAddRequest();
        add.setContentIds(List.of(anchor.getContentId(), pending.getContentId()));
        topicService.addContents(topic.getId(), add);
        topicService.publish(topic.getId());

        assertThat(topicService.publicDetail(topic.getId()).getContents())
                .extracting(PublicTopicContentVO::getId)
                .contains(anchor.getContentId())
                .doesNotContain(pending.getContentId());

        contentService.schedulePublish(pending.getContentId(), schedule(LocalDateTime.now().plusHours(1)));
        markDue(pending.getContentId());
        contentPublishJob.runOnce();

        assertThat(topicService.publicDetail(topic.getId()).getContents())
                .extracting(PublicTopicContentVO::getId)
                .contains(anchor.getContentId(), pending.getContentId());
    }

    @Test
    void scheduleRequiresPublishPermission() {
        ArticleDetailVO article = articleService.create(article("无权限"));
        TestAuth.login(PermissionCodes.CONTENT_VIEW);
        assertThatThrownBy(() -> contentService.schedulePublish(
                article.getContentId(), schedule(LocalDateTime.now().plusHours(1))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private void markDue(Long id) {
        contentMapper.update(null, Wrappers.<Content>lambdaUpdate()
                .set(Content::getScheduledPublishTime, LocalDateTime.now().minusMinutes(1))
                .eq(Content::getId, id));
    }

    private SchedulePublishRequest schedule(LocalDateTime time) {
        SchedulePublishRequest request = new SchedulePublishRequest();
        request.setPublishTime(time.withNano(0).toString().replace('T', ' '));
        if (request.getPublishTime().length() == 16) {
            request.setPublishTime(request.getPublishTime() + ":00");
        }
        return request;
    }

    private ArticleCreateRequest article(String title) {
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setSort(1);
        request.setBody("正文");
        request.setAuthor("老师");
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

    private QuestionCreateRequest question(String title) {
        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setSort(1);
        request.setQuestionType(QuestionType.ANSWER);
        request.setQuestionText("题干");
        request.setAnswerText("答案");
        return request;
    }

    private WeeklyCreateRequest weekly(String title) {
        WeeklyCreateRequest request = new WeeklyCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setSort(1);
        request.setWeekLabel("2026年第39周");
        request.setQuestionText("题干");
        request.setAnswerText("答案");
        return request;
    }

    private DocumentCreateRequest document(String title) {
        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setSort(1);
        request.setFileUrl("https://example.com/files/a.pdf");
        request.setFileName("a.pdf");
        request.setFileSize(128L);
        request.setFileType("pdf");
        return request;
    }

    private com.xxedu.learning.modules.content.dto.ContentCreateRequest bare(String title) {
        var request = new com.xxedu.learning.modules.content.dto.ContentCreateRequest();
        request.setContentType(com.xxedu.learning.modules.content.enums.ContentType.ARTICLE);
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setSort(1);
        return request;
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
}

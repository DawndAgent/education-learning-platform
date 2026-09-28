package com.xxedu.learning.modules.topic;

import com.xxedu.learning.common.api.PageResult;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.article.vo.ArticleDetailVO;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.topic.dto.TopicContentAddRequest;
import com.xxedu.learning.modules.topic.dto.TopicContentSortRequest;
import com.xxedu.learning.modules.topic.dto.TopicCreateRequest;
import com.xxedu.learning.modules.topic.dto.TopicQueryRequest;
import com.xxedu.learning.modules.topic.dto.TopicUpdateRequest;
import com.xxedu.learning.modules.topic.enums.TopicStatus;
import com.xxedu.learning.modules.topic.service.TopicService;
import com.xxedu.learning.modules.topic.vo.TopicContentItemVO;
import com.xxedu.learning.modules.topic.vo.TopicDetailVO;
import com.xxedu.learning.modules.topic.vo.TopicListVO;
import com.xxedu.learning.modules.video.dto.VideoCreateRequest;
import com.xxedu.learning.modules.video.enums.VideoSourceType;
import com.xxedu.learning.modules.video.service.VideoService;
import com.xxedu.learning.modules.video.vo.VideoDetailVO;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class TopicServiceTest extends IntegrationTestSupport {

    @Autowired
    private TopicService topicService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void createUpdateListAndRefuseDeleteWhenPublished() {
        TopicDetailVO created = topicService.create(topic("KET 词汇专题", "KET_VOCAB_01", 11L, 2));
        assertThat(created.getStatus()).isEqualTo(TopicStatus.DRAFT);
        assertThat(created.getCode()).isEqualTo("KET_VOCAB_01");
        assertThat(created.getCategoryName()).isEqualTo("KET/PET备考资料");
        assertThat(created.getContentCount()).isZero();

        TopicUpdateRequest update = new TopicUpdateRequest();
        update.setName("KET 高频词汇");
        update.setCoverUrl("https://cdn.example.com/cover.png");
        update.setSummary("词汇汇总");
        update.setCategoryId(12L);
        update.setSort(5);
        TopicDetailVO updated = topicService.update(created.getId(), update);
        assertThat(updated.getName()).isEqualTo("KET 高频词汇");
        assertThat(updated.getCode()).isEqualTo("KET_VOCAB_01");
        assertThat(updated.getCategoryId()).isEqualTo(12L);
        assertThat(updated.getSort()).isEqualTo(5);

        ArticleDetailVO article = articleService.create(article("专题文章"));
        contentService.publish(article.getContentId());
        topicService.addContents(created.getId(), contents(article.getContentId()));
        topicService.publish(created.getId());

        TopicQueryRequest query = new TopicQueryRequest();
        query.setPageNum(1);
        query.setPageSize(20);
        query.setKeyword("高频");
        query.setStatus(TopicStatus.PUBLISHED);
        PageResult<TopicListVO> page = topicService.adminPage(query);
        assertThat(page.getRecords()).extracting(TopicListVO::getName).containsExactly("KET 高频词汇");
        assertThat(page.getRecords().get(0).getContentCount()).isEqualTo(1);

        assertThatThrownBy(() -> topicService.delete(created.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先下线专题后再删除");

        topicService.offline(created.getId());
        topicService.delete(created.getId());
        assertThatThrownBy(() -> topicService.adminDetail(created.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("专题不存在");
        Integer contentDeleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM content WHERE id = ?", Integer.class, article.getContentId());
        assertThat(contentDeleted).isZero();
    }

    @Test
    void duplicateCodeRejectedAndSoftDeletedRelationRestored() {
        topicService.create(topic("编码占用", "TOPIC_CODE_X", 11L, 1));
        assertThatThrownBy(() -> topicService.create(topic("编码冲突", "TOPIC_CODE_X", 11L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("专题编码已存在");

        TopicDetailVO topic = topicService.create(topic("关联恢复", "TOPIC_RESTORE", 11L, 1));
        ArticleDetailVO article = articleService.create(article("可恢复内容"));
        topicService.addContents(topic.getId(), contents(article.getContentId()));
        topicService.removeContent(topic.getId(), article.getContentId());
        assertThat(topicService.listContents(topic.getId())).isEmpty();

        topicService.addContents(topic.getId(), contents(article.getContentId()));
        assertThat(topicService.listContents(topic.getId()))
                .extracting(TopicContentItemVO::getContentId)
                .containsExactly(article.getContentId());
        Long relationRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM topic_content WHERE topic_id = ? AND content_id = ?",
                Long.class, topic.getId(), article.getContentId());
        assertThat(relationRows).isEqualTo(1);
    }

    @Test
    void addSortRemoveContentsAndPublishRules() {
        TopicDetailVO topic = topicService.create(topic("发布规则专题", "TOPIC_PUBLISH_RULE", 11L, 1));
        ArticleDetailVO draft = articleService.create(article("草稿文章"));
        ArticleDetailVO published = articleService.create(article("已发文章"));
        VideoDetailVO video = videoService.create(video("已发视频"));
        contentService.publish(published.getContentId());
        contentService.publish(video.getContentId());

        assertThatThrownBy(() -> topicService.publish(topic.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("发布专题至少需要一篇已发布的内容");

        topicService.addContents(topic.getId(), contents(draft.getContentId()));
        assertThatThrownBy(() -> topicService.publish(topic.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("发布专题至少需要一篇已发布的内容");

        topicService.addContents(topic.getId(), contents(published.getContentId(), video.getContentId()));
        List<TopicContentItemVO> beforeSort = topicService.listContents(topic.getId());
        assertThat(beforeSort).hasSize(3);

        TopicContentSortRequest sortRequest = new TopicContentSortRequest();
        TopicContentSortRequest.Item first = new TopicContentSortRequest.Item();
        first.setContentId(video.getContentId());
        first.setSort(1);
        TopicContentSortRequest.Item second = new TopicContentSortRequest.Item();
        second.setContentId(published.getContentId());
        second.setSort(2);
        TopicContentSortRequest.Item third = new TopicContentSortRequest.Item();
        third.setContentId(draft.getContentId());
        third.setSort(3);
        sortRequest.setItems(List.of(first, second, third));
        List<TopicContentItemVO> sorted = topicService.sortContents(topic.getId(), sortRequest);
        assertThat(sorted).extracting(TopicContentItemVO::getContentId)
                .containsExactly(video.getContentId(), published.getContentId(), draft.getContentId());

        TopicContentSortRequest dup = new TopicContentSortRequest();
        TopicContentSortRequest.Item a = new TopicContentSortRequest.Item();
        a.setContentId(published.getContentId());
        a.setSort(1);
        TopicContentSortRequest.Item b = new TopicContentSortRequest.Item();
        b.setContentId(published.getContentId());
        b.setSort(2);
        dup.setItems(List.of(a, b));
        assertThatThrownBy(() -> topicService.sortContents(topic.getId(), dup))
                .isInstanceOf(BusinessException.class)
                .hasMessage("排序项不能包含重复内容");

        TopicDetailVO publishedTopic = topicService.publish(topic.getId());
        LocalDateTime publishTime = publishedTopic.getPublishTime();
        assertThat(publishedTopic.getStatus()).isEqualTo(TopicStatus.PUBLISHED);
        assertThat(topicService.publish(topic.getId()).getPublishTime()).isEqualTo(publishTime);

        topicService.removeContent(topic.getId(), draft.getContentId());
        assertThat(topicService.listContents(topic.getId())).hasSize(2);
    }

    @Test
    void draftCanGoOfflineAndPermissionsAreEnforced() {
        TopicDetailVO topic = topicService.create(topic("权限专题", "TOPIC_PERM", 11L, 1));
        TopicDetailVO offline = topicService.offline(topic.getId());
        assertThat(offline.getStatus()).isEqualTo(TopicStatus.OFFLINE);

        TestAuth.login(PermissionCodes.TOPIC_VIEW);
        assertThatThrownBy(() -> topicService.create(topic("无权限", "TOPIC_NO_PERM", 11L, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private TopicCreateRequest topic(String name, String code, Long categoryId, int sort) {
        TopicCreateRequest request = new TopicCreateRequest();
        request.setName(name);
        request.setCode(code);
        request.setCategoryId(categoryId);
        request.setSort(sort);
        request.setSummary("简介");
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

    private TopicContentAddRequest contents(Long... ids) {
        TopicContentAddRequest request = new TopicContentAddRequest();
        request.setContentIds(List.of(ids));
        return request;
    }
}

package com.xxedu.learning.modules.topic;

import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.article.vo.ArticleDetailVO;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.topic.dto.TopicContentAddRequest;
import com.xxedu.learning.modules.topic.dto.TopicCreateRequest;
import com.xxedu.learning.modules.topic.dto.TopicQueryRequest;
import com.xxedu.learning.modules.topic.service.TopicService;
import com.xxedu.learning.modules.topic.vo.PublicTopicContentVO;
import com.xxedu.learning.modules.topic.vo.PublicTopicDetailVO;
import com.xxedu.learning.modules.topic.vo.PublicTopicListVO;
import com.xxedu.learning.modules.topic.vo.TopicDetailVO;
import com.xxedu.learning.modules.video.dto.VideoCreateRequest;
import com.xxedu.learning.modules.video.enums.VideoSourceType;
import com.xxedu.learning.modules.video.service.VideoService;
import com.xxedu.learning.modules.video.vo.VideoDetailVO;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class TopicPublicApiTest extends IntegrationTestSupport {

    @Autowired
    private TopicService topicService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private ContentService contentService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void publicApiReturnsOnlyPublishedTopicAndPublishedContents() throws Exception {
        TopicDetailVO draftTopic = topicService.create(topic("草稿专题", "PUB_DRAFT_TOPIC", 1));
        ArticleDetailVO alone = articleService.create(article("无关已发"));
        contentService.publish(alone.getContentId());

        TopicDetailVO topic = topicService.create(topic("公开专题", "PUB_OPEN_TOPIC", 3));
        ArticleDetailVO publishedArticle = articleService.create(article("专题已发文章"));
        ArticleDetailVO draftArticle = articleService.create(article("专题草稿文章"));
        VideoDetailVO publishedVideo = videoService.create(video("专题已发视频"));
        contentService.publish(publishedArticle.getContentId());
        contentService.publish(publishedVideo.getContentId());

        TopicContentAddRequest add = new TopicContentAddRequest();
        add.setContentIds(List.of(
                publishedArticle.getContentId(),
                draftArticle.getContentId(),
                publishedVideo.getContentId()));
        topicService.addContents(topic.getId(), add);
        topicService.publish(topic.getId());

        mockMvc.perform(get("/api/topics").param("keyword", "公开"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("公开专题"))
                .andExpect(jsonPath("$.data.records[0].id").value(String.valueOf(topic.getId())));

        mockMvc.perform(get("/api/topics/" + topic.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("公开专题"))
                .andExpect(jsonPath("$.data.category.name").value("KET/PET备考资料"))
                .andExpect(jsonPath("$.data.contents.length()").value(2))
                .andExpect(jsonPath("$.data.contents[0].title").value("专题已发文章"))
                .andExpect(jsonPath("$.data.contents[1].title").value("专题已发视频"));

        mockMvc.perform(get("/api/topics/" + draftTopic.getId()))
                .andExpect(status().isNotFound());

        TestAuth.loginOperator();

        PublicTopicDetailVO detail = topicService.publicDetail(topic.getId());
        assertThat(detail.getContents()).extracting(PublicTopicContentVO::getTitle)
                .containsExactly("专题已发文章", "专题已发视频")
                .doesNotContain("专题草稿文章");

        contentService.offline(publishedArticle.getContentId());
        assertThat(topicService.publicDetail(topic.getId()).getContents())
                .extracting(PublicTopicContentVO::getTitle)
                .containsExactly("专题已发视频");

        contentService.publish(publishedArticle.getContentId());
        assertThat(topicService.publicDetail(topic.getId()).getContents())
                .extracting(PublicTopicContentVO::getTitle)
                .contains("专题已发文章", "专题已发视频");

        topicService.offline(topic.getId());
        assertThatThrownBy(() -> topicService.publicDetail(topic.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("专题不存在");
        TopicQueryRequest query = new TopicQueryRequest();
        query.setPageNum(1);
        query.setPageSize(20);
        query.setKeyword("公开");
        assertThat(topicService.publicPage(query).getRecords()).isEmpty();
    }

    @Test
    void featuredCapsAtFourAndOrdersBySortThenPublishTime() throws Exception {
        for (int i = 1; i <= 5; i++) {
            TopicDetailVO topic = topicService.create(topic("精选" + i, "FEAT_" + i, 10 - i));
            ArticleDetailVO article = articleService.create(article("精选内容" + i));
            contentService.publish(article.getContentId());
            TopicContentAddRequest add = new TopicContentAddRequest();
            add.setContentIds(List.of(article.getContentId()));
            topicService.addContents(topic.getId(), add);
            topicService.publish(topic.getId());
        }

        mockMvc.perform(get("/api/topics/featured").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].name").value("精选5"));

        List<PublicTopicListVO> featured = topicService.publicFeatured(4);
        assertThat(featured).hasSize(4);
        assertThat(featured.get(0).getSort()).isLessThanOrEqualTo(featured.get(1).getSort());
    }

    private TopicCreateRequest topic(String name, String code, int sort) {
        TopicCreateRequest request = new TopicCreateRequest();
        request.setName(name);
        request.setCode(code);
        request.setCategoryId(11L);
        request.setSort(sort);
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
}

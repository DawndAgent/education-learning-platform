package com.xxedu.learning.modules.content;

import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.article.vo.ArticleDetailVO;
import com.xxedu.learning.modules.content.dto.ContentUpdateRequest;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.content.vo.ContentDetailVO;
import com.xxedu.learning.modules.video.dto.VideoCreateRequest;
import com.xxedu.learning.modules.video.enums.VideoSourceType;
import com.xxedu.learning.modules.video.service.VideoService;
import com.xxedu.learning.modules.video.vo.VideoDetailVO;
import com.xxedu.learning.security.JwtTokenService;
import com.xxedu.learning.security.LoginUser;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ContentProductionLoopTest extends IntegrationTestSupport {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void articleCreateDraftPublishOfflineRepublishKeepsPublicVisibility() throws Exception {
        ArticleDetailVO draft = articleService.create(article("生产闭环文章"));
        assertThat(draft.getStatus()).isEqualTo(ContentStatus.DRAFT);

        mockMvc.perform(get("/api/content/" + draft.getContentId()))
                .andExpect(status().isNotFound());

        TestAuth.loginOperator();
        ContentDetailVO published = contentService.publish(draft.getContentId());
        assertThat(published.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        LocalDateTime firstPublish = published.getPublishTime();
        assertThat(firstPublish).isNotNull();

        mockMvc.perform(get("/api/content/" + draft.getContentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("生产闭环文章"));
        mockMvc.perform(get("/api/articles/" + draft.getContentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("正文"));

        mockMvc.perform(post("/api/content/" + draft.getContentId() + "/view"))
                .andExpect(status().isOk());
        TestAuth.loginOperator();
        assertThat(contentService.publicDetail(draft.getContentId()).getViewCount()).isEqualTo(1L);

        ContentDetailVO edited = contentService.update(draft.getContentId(), updateTitle(published, "生产闭环文章-修订"));
        assertThat(edited.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(edited.getPublishTime()).isEqualTo(firstPublish);

        contentService.offline(draft.getContentId());
        mockMvc.perform(get("/api/content/" + draft.getContentId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/content/" + draft.getContentId() + "/view"))
                .andExpect(status().isNotFound());

        TestAuth.loginOperator();
        ContentDetailVO again = contentService.publish(draft.getContentId());
        assertThat(again.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(again.getPublishTime()).isAfterOrEqualTo(firstPublish);
        mockMvc.perform(get("/api/content/" + draft.getContentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("生产闭环文章-修订"));
    }

    @Test
    void videoCreatePublishOfflineRepublish() throws Exception {
        VideoDetailVO draft = videoService.create(video("生产闭环视频"));
        assertThat(draft.getStatus()).isEqualTo(ContentStatus.DRAFT);

        contentService.publish(draft.getContentId());
        mockMvc.perform(get("/api/videos/" + draft.getContentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.qrCodeUrl").value("https://example.com/qr.png"));

        TestAuth.loginOperator();
        contentService.offline(draft.getContentId());
        mockMvc.perform(get("/api/videos/" + draft.getContentId()))
                .andExpect(status().isNotFound());

        TestAuth.loginOperator();
        contentService.publish(draft.getContentId());
        mockMvc.perform(get("/api/content").param("keyword", "生产闭环视频"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void adminContentCreateEndpointRejected() throws Exception {
        mockMvc.perform(post("/admin/api/contents")
                        .header("Authorization", "Bearer " + token(PermissionCodes.CONTENT_CREATE, PermissionCodes.CONTENT_VIEW))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"ARTICLE","categoryId":11,"title":"壳内容","sort":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请使用对应类型接口创建内容"));
    }

    @Test
    void publishIsIdempotentForAlreadyPublished() {
        ArticleDetailVO draft = articleService.create(article("幂等发布"));
        ContentDetailVO first = contentService.publish(draft.getContentId());
        LocalDateTime publishTime = first.getPublishTime();
        ContentDetailVO second = contentService.publish(draft.getContentId());
        assertThat(second.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(second.getPublishTime()).isEqualTo(publishTime);
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
        request.setCategoryId(13L);
        request.setTitle(title);
        request.setSort(1);
        request.setSourceType(VideoSourceType.WECHAT_CHANNEL);
        request.setVideoUrl("https://channels.weixin.qq.com/x");
        request.setQrCodeUrl("https://example.com/qr.png");
        request.setDuration(60);
        return request;
    }

    private ContentUpdateRequest updateTitle(ContentDetailVO current, String title) {
        ContentUpdateRequest request = new ContentUpdateRequest();
        request.setContentType(ContentType.ARTICLE);
        request.setCategoryId(current.getCategoryId());
        request.setTitle(title);
        request.setCoverUrl(current.getCoverUrl());
        request.setSummary(current.getSummary());
        request.setSort(current.getSort());
        return request;
    }

    private String token(String... permissions) {
        return jwtTokenService.issue(new LoginUser(8L, "admin", ClientType.ADMIN, Set.of(permissions)));
    }
}

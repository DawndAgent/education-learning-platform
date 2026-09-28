package com.xxedu.learning.modules.content;

import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.article.vo.ArticleDetailVO;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.content.vo.ContentBatchResultVO;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ContentOpsApiTest extends IntegrationTestSupport {

    @Autowired
    private ContentService contentService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void duplicateArticleCreatesDraftWithoutStats() {
        ArticleDetailVO source = articleService.create(article("原文章"));
        contentService.publish(source.getContentId());
        jdbcTemplate.update("UPDATE content SET view_count = 9, favorite_count = 4 WHERE id = ?", source.getContentId());

        ContentDetailVO copy = contentService.duplicate(source.getContentId());
        assertThat(copy.getId()).isNotEqualTo(source.getContentId());
        assertThat(copy.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(copy.getPublishTime()).isNull();
        assertThat(copy.getViewCount()).isZero();
        assertThat(copy.getFavoriteCount()).isZero();
        assertThat(copy.getTitle()).isEqualTo("原文章");
        assertThat(copy.getContentType()).isEqualTo(ContentType.ARTICLE);

        ArticleDetailVO articleCopy = articleService.adminDetail(copy.getId());
        assertThat(articleCopy.getBody()).isEqualTo("正文内容");
        assertThat(articleCopy.getAuthor()).isEqualTo("老师");
        assertThat(articleCopy.getStatus()).isEqualTo(ContentStatus.DRAFT);
    }

    @Test
    void duplicateVideoCreatesDraft() {
        VideoDetailVO source = videoService.create(video("原视频"));
        contentService.publish(source.getContentId());

        ContentDetailVO copy = contentService.duplicate(source.getContentId());
        assertThat(copy.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(copy.getContentType()).isEqualTo(ContentType.VIDEO);

        VideoDetailVO videoCopy = videoService.adminDetail(copy.getId());
        assertThat(videoCopy.getVideoUrl()).isEqualTo("https://channels.weixin.qq.com/example");
        assertThat(videoCopy.getQrCodeUrl()).isEqualTo("https://example.com/qr.png");
        assertThat(videoCopy.getStatus()).isEqualTo(ContentStatus.DRAFT);
    }

    @Test
    void duplicateMissingContentFails() {
        assertThatThrownBy(() -> contentService.duplicate(999999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容不存在");
    }

    @Test
    void batchOfflineAllPublishedSucceeds() {
        Long a = articleService.create(article("下线A")).getContentId();
        Long b = articleService.create(article("下线B")).getContentId();
        contentService.publish(a);
        contentService.publish(b);

        ContentBatchResultVO result = contentService.batchOffline(List.of(a, b));
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailedCount()).isZero();
        assertThat(contentService.adminDetail(a).getStatus()).isEqualTo(ContentStatus.OFFLINE);
        assertThat(contentService.adminDetail(b).getStatus()).isEqualTo(ContentStatus.OFFLINE);
    }

    @Test
    void batchOfflineRejectsMixedStatus() {
        Long published = articleService.create(article("已发")).getContentId();
        Long draft = articleService.create(article("草稿")).getContentId();
        contentService.publish(published);

        assertThatThrownBy(() -> contentService.batchOffline(List.of(published, draft)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只能下线已发布内容");
        assertThat(contentService.adminDetail(published).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
    }

    @Test
    void batchDeleteRejectsPublishedAndDeletesDraftOffline() {
        Long draft = articleService.create(article("删草稿")).getContentId();
        Long offline = articleService.create(article("删下线")).getContentId();
        Long published = articleService.create(article("删发布")).getContentId();
        contentService.publish(offline);
        contentService.offline(offline);
        contentService.publish(published);

        assertThatThrownBy(() -> contentService.batchDelete(List.of(draft, published)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(contentService.adminDetail(draft).getId()).isEqualTo(draft);

        ContentBatchResultVO result = contentService.batchDelete(List.of(draft, offline));
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThatThrownBy(() -> contentService.adminDetail(draft))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容不存在");
    }

    @Test
    void batchAndDuplicatePermissionChecks() throws Exception {
        Long id = articleService.create(article("权限文")).getContentId();
        contentService.publish(id);

        mockMvc.perform(post("/admin/api/contents/batch-offline")
                        .header("Authorization", "Bearer " + token(PermissionCodes.CONTENT_VIEW))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + id + "\"]}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/api/contents/" + id + "/duplicate")
                        .header("Authorization", "Bearer " + token(PermissionCodes.CONTENT_VIEW))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/api/contents/" + id + "/duplicate")
                        .header("Authorization", "Bearer " + token(PermissionCodes.CONTENT_CREATE))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    private ArticleCreateRequest article(String title) {
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setSort(1);
        request.setBody("正文内容");
        request.setAuthor("老师");
        request.setSource("内部");
        return request;
    }

    private VideoCreateRequest video(String title) {
        VideoCreateRequest request = new VideoCreateRequest();
        request.setCategoryId(11L);
        request.setTitle(title);
        request.setCoverUrl("https://example.com/cover.png");
        request.setSummary("摘要");
        request.setSort(1);
        request.setSourceType(VideoSourceType.WECHAT_CHANNEL);
        request.setVideoUrl("https://channels.weixin.qq.com/example");
        request.setQrCodeUrl("https://example.com/qr.png");
        request.setDuration(60);
        return request;
    }

    private String token(String... permissions) {
        return jwtTokenService.issue(new LoginUser(8L, "admin", ClientType.ADMIN, Set.of(permissions)));
    }
}

package com.xxedu.learning.modules.video;

import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.security.JwtTokenService;
import com.xxedu.learning.security.LoginUser;
import com.xxedu.learning.modules.content.dto.ContentCreateRequest;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.security.PermissionCodes;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.content.vo.ContentDetailVO;
import com.xxedu.learning.modules.video.dto.VideoCreateRequest;
import com.xxedu.learning.modules.video.dto.VideoUpdateRequest;
import com.xxedu.learning.modules.video.enums.VideoSourceType;
import com.xxedu.learning.modules.video.service.VideoService;
import com.xxedu.learning.modules.video.vo.VideoDetailVO;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class VideoServiceTest extends IntegrationTestSupport {

    @Autowired
    private VideoService videoService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void createCopiesCatalogFieldsAndPublishSyncsStatus() {
        VideoDetailVO created = videoService.create(create("听力课"));

        assertThat(created.getTitle()).isEqualTo("听力课");
        assertThat(created.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(created.getSourceType()).isEqualTo(VideoSourceType.WECHAT_CHANNEL);
        assertThat(jdbcTemplate.queryForObject(
                "select title from video where content_id = ?", String.class, created.getContentId()))
                .isEqualTo("听力课");
        assertThatThrownBy(() -> videoService.publicDetail(created.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("视频不存在");

        ContentDetailVO published = contentService.publish(created.getContentId());
        assertThat(published.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(videoService.publicDetail(created.getContentId()).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(jdbcTemplate.queryForObject(
                "select status from video where content_id = ?", String.class, created.getContentId()))
                .isEqualTo("PUBLISHED");
    }

    @Test
    void updateKeepsContentAndVideoTitlesTogether() {
        VideoDetailVO created = videoService.create(create("旧标题"));
        VideoUpdateRequest update = new VideoUpdateRequest();
        update.setCategoryId(13L);
        update.setTitle("新标题");
        update.setCoverUrl("https://example.com/new.png");
        update.setSort(4);
        update.setSourceType(VideoSourceType.TENCENT_VIDEO);
        update.setVideoUrl("https://v.qq.com/new");
        update.setDuration(90);

        VideoDetailVO updated = videoService.update(created.getContentId(), update);

        assertThat(updated.getTitle()).isEqualTo("新标题");
        assertThat(updated.getCoverUrl()).isEqualTo("https://example.com/new.png");
        assertThat(updated.getVideoUrl()).isEqualTo("https://v.qq.com/new");
        assertThat(contentService.adminDetail(created.getContentId()).getTitle()).isEqualTo("新标题");
        assertThat(jdbcTemplate.queryForObject(
                "select title from video where content_id = ?", String.class, created.getContentId()))
                .isEqualTo("新标题");
    }

    @Test
    void editorFillsVideoAndQrForContentCreatedWithoutVideo() {
        ContentCreateRequest content = new ContentCreateRequest();
        content.setContentType(ContentType.VIDEO);
        content.setCategoryId(13L);
        content.setTitle("仅主表视频");
        content.setSort(2);
        ContentDetailVO created = contentService.create(content);

        assertThat(videoService.adminDetail(created.getId()).getVideoUrl()).isNull();
        assertThat(videoService.adminDetail(created.getId()).getQrCodeUrl()).isNull();

        VideoUpdateRequest update = new VideoUpdateRequest();
        update.setCategoryId(13L);
        update.setTitle("仅主表视频");
        update.setSort(2);
        update.setSourceType(VideoSourceType.WECHAT_CHANNEL);
        update.setVideoUrl(" https://channels.weixin.qq.com/live04e ");
        update.setQrCodeUrl(" https://example.com/qr.png ");
        update.setDuration(30);
        VideoDetailVO saved = videoService.update(created.getId(), update);

        assertThat(saved.getVideoUrl()).isEqualTo("https://channels.weixin.qq.com/live04e");
        assertThat(saved.getQrCodeUrl()).isEqualTo("https://example.com/qr.png");
        assertThat(saved.getDuration()).isEqualTo(30);
        assertThat(jdbcTemplate.queryForObject(
                "select qr_code_url from video where content_id = ?", String.class, created.getId()))
                .isEqualTo("https://example.com/qr.png");
    }

    @Test
    void draftAllowsEmptyUrlsAndPublishRequiresLegalUrls() {
        VideoCreateRequest draft = create("草稿视频");
        draft.setVideoUrl(" ");
        draft.setQrCodeUrl(null);
        VideoDetailVO created = videoService.create(draft);
        assertThat(created.getVideoUrl()).isEmpty();
        assertThatThrownBy(() -> contentService.publish(created.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("视频地址不能为空");

        VideoUpdateRequest withUrl = new VideoUpdateRequest();
        withUrl.setCategoryId(13L);
        withUrl.setTitle("草稿视频");
        withUrl.setSort(1);
        withUrl.setSourceType(VideoSourceType.WECHAT_CHANNEL);
        withUrl.setVideoUrl("https://channels.weixin.qq.com/example");
        videoService.update(created.getContentId(), withUrl);
        assertThatThrownBy(() -> contentService.publish(created.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("二维码地址不能为空");

        withUrl.setQrCodeUrl("https://example.com/qr.png");
        videoService.update(created.getContentId(), withUrl);
        assertThat(contentService.publish(created.getContentId()).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(videoService.publicDetail(created.getContentId()).getQrCodeUrl()).isEqualTo("https://example.com/qr.png");
    }

    @Test
    void localVideoPublishesWithoutQrCode() {
        VideoCreateRequest request = create("本地课");
        request.setSourceType(VideoSourceType.LOCAL);
        request.setVideoUrl("/uploads/videos/local/demo.mp4");
        request.setQrCodeUrl(null);
        VideoDetailVO created = videoService.create(request);
        assertThat(contentService.publish(created.getContentId()).getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(videoService.publicDetail(created.getContentId()).getVideoUrl())
                .isEqualTo("/uploads/videos/local/demo.mp4");
        assertThat(videoService.publicDetail(created.getContentId()).getSourceType())
                .isEqualTo(VideoSourceType.LOCAL);
    }

    @Test
    void localVideoPublishRequiresMediaUrl() {
        VideoCreateRequest request = create("本地坏地址");
        request.setSourceType(VideoSourceType.LOCAL);
        request.setVideoUrl(" ");
        request.setQrCodeUrl(null);
        VideoDetailVO created = videoService.create(request);
        assertThatThrownBy(() -> contentService.publish(created.getContentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("视频地址不能为空");
    }

    @Test
    void rejectsIllegalVideoAndQrUrls() {
        VideoCreateRequest badVideo = create("坏地址");
        badVideo.setVideoUrl("javascript:alert(1)");
        assertThatThrownBy(() -> videoService.create(badVideo))
                .isInstanceOf(BusinessException.class)
                .hasMessage("视频地址不合法");

        VideoCreateRequest badQr = create("坏二维码");
        badQr.setQrCodeUrl("not-a-url");
        assertThatThrownBy(() -> videoService.create(badQr))
                .isInstanceOf(BusinessException.class)
                .hasMessage("二维码地址不合法");
    }

    @Test
    void rejectsUnknownSourceType() throws Exception {
        String token = jwtTokenService.issue(new LoginUser(1L, "admin", ClientType.ADMIN,
                Set.of(PermissionCodes.VIDEO_MANAGE)));
        mockMvc.perform(post("/admin/api/videos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":13,\"title\":\"坏来源\",\"sort\":1,"
                                + "\"sourceType\":\"BILIBILI\",\"videoUrl\":\"https://example.com/a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求体无法解析"));
    }

    @Test
    void videoUpdateRequiresVideoManage() {
        VideoDetailVO created = videoService.create(create("权限视频"));
        TestAuth.login(PermissionCodes.CONTENT_UPDATE);

        VideoUpdateRequest update = new VideoUpdateRequest();
        update.setCategoryId(13L);
        update.setTitle("权限视频");
        update.setSort(1);
        update.setSourceType(VideoSourceType.TENCENT_VIDEO);
        update.setVideoUrl("https://v.qq.com/example");
        assertThatThrownBy(() -> videoService.update(created.getContentId(), update))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private VideoCreateRequest create(String title) {
        VideoCreateRequest request = new VideoCreateRequest();
        request.setCategoryId(13L);
        request.setTitle(title);
        request.setCoverUrl("https://example.com/cover.png");
        request.setSummary("摘要");
        request.setSort(1);
        request.setSourceType(VideoSourceType.WECHAT_CHANNEL);
        request.setVideoUrl("https://channels.weixin.qq.com/example");
        request.setQrCodeUrl("https://example.com/qr.png");
        request.setDuration(120);
        return request;
    }
}

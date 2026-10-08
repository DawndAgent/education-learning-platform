package com.xxedu.learning.modules.video;

import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.modules.video.dto.VideoCreateRequest;
import com.xxedu.learning.modules.video.enums.VideoSourceType;
import com.xxedu.learning.modules.video.service.VideoMiniprogramQrService;
import com.xxedu.learning.modules.video.service.VideoService;
import com.xxedu.learning.modules.video.vo.MiniprogramQrVO;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class VideoMiniprogramQrServiceTest extends IntegrationTestSupport {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoMiniprogramQrService videoMiniprogramQrService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void generatesAndCachesMiniprogramQr() {
        VideoDetailVO created = videoService.create(create("讲解视频"));

        MiniprogramQrVO first = videoMiniprogramQrService.ensureMiniprogramQr(created.getContentId(), false);
        assertThat(first.getUrl()).isNotBlank();
        assertThat(first.isMocked()).isTrue();
        assertThat(first.getTitle()).isEqualTo("讲解视频");

        String stored = jdbcTemplate.queryForObject(
                "select miniprogram_qr_url from video where content_id = ?",
                String.class, created.getContentId());
        assertThat(stored).isEqualTo(first.getUrl());

        MiniprogramQrVO second = videoMiniprogramQrService.ensureMiniprogramQr(created.getContentId(), false);
        assertThat(second.getUrl()).isEqualTo(first.getUrl());
    }

    @Test
    void adminEndpointReturnsQr() throws Exception {
        VideoDetailVO created = videoService.create(create("接口视频"));
        String token = jwtTokenService.issue(new LoginUser(1L, "admin", ClientType.ADMIN,
                Set.of(PermissionCodes.FILE_UPLOAD)));

        mockMvc.perform(post("/admin/api/videos/" + created.getContentId() + "/miniprogram-qrcode")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentId").value(created.getContentId()))
                .andExpect(jsonPath("$.data.url").isNotEmpty())
                .andExpect(jsonPath("$.data.mocked").value(true));
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
        request.setDuration(60);
        return request;
    }
}

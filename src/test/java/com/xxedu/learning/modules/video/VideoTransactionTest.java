package com.xxedu.learning.modules.video;

import com.xxedu.learning.modules.content.dto.ContentCreateRequest;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.video.dto.VideoCreateRequest;
import com.xxedu.learning.modules.video.dto.VideoUpdateRequest;
import com.xxedu.learning.modules.video.enums.VideoSourceType;
import com.xxedu.learning.modules.video.mapper.VideoMapper;
import com.xxedu.learning.modules.video.service.VideoService;
import com.xxedu.learning.support.IntegrationTestSupport;
import com.xxedu.learning.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class VideoTransactionTest extends IntegrationTestSupport {

    @Autowired
    private VideoService videoService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private VideoMapper videoMapper;

    @BeforeEach
    void login() {
        TestAuth.loginOperator();
    }

    @Test
    void contentInsertRollsBackWhenVideoInsertFails() {
        when(videoMapper.insert(any(com.xxedu.learning.modules.video.entity.Video.class)))
                .thenThrow(new RuntimeException("video insert failed"));
        int before = count("content");

        assertThatThrownBy(() -> videoService.create(request()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("video insert failed");

        assertThat(count("content")).isEqualTo(before);
        assertThat(count("video")).isEqualTo(0);
    }

    @Test
    void contentUpdateRollsBackWhenVideoInsertFails() {
        when(videoMapper.insert(any(com.xxedu.learning.modules.video.entity.Video.class)))
                .thenThrow(new RuntimeException("video insert failed"));
        ContentCreateRequest content = new ContentCreateRequest();
        content.setCategoryId(13L);
        content.setTitle("原视频");
        content.setContentType(ContentType.VIDEO);
        content.setSort(1);
        Long id = contentService.create(content).getId();
        try {
            VideoUpdateRequest update = new VideoUpdateRequest();
            update.setCategoryId(13L);
            update.setTitle("不应保留");
            update.setSort(1);
            update.setSourceType(VideoSourceType.WECHAT_CHANNEL);
            update.setVideoUrl("https://channels.weixin.qq.com/example");
            assertThatThrownBy(() -> videoService.update(id, update))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("video insert failed");
            assertThat(contentService.adminDetail(id).getTitle()).isEqualTo("原视频");
        } finally {
            contentService.delete(id);
        }
    }

    private int count(String table) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + table + " where deleted = 0", Integer.class);
        return count == null ? 0 : count;
    }

    private VideoCreateRequest request() {
        VideoCreateRequest request = new VideoCreateRequest();
        request.setCategoryId(13L);
        request.setTitle("回滚视频");
        request.setSort(1);
        request.setSourceType(VideoSourceType.TENCENT_VIDEO);
        request.setVideoUrl("https://v.qq.com/example");
        return request;
    }
}

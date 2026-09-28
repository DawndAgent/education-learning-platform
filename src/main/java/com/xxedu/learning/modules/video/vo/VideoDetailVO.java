package com.xxedu.learning.modules.video.vo;

import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.video.enums.VideoSourceType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VideoDetailVO {

    private Long contentId;
    private String title;
    private Long categoryId;
    private String coverUrl;
    private String summary;
    private ContentStatus status;
    private Integer sort;
    private LocalDateTime publishTime;
    private LocalDateTime scheduledPublishTime;
    private VideoSourceType sourceType;
    private String videoUrl;
    private String qrCodeUrl;
    private Integer duration;
}

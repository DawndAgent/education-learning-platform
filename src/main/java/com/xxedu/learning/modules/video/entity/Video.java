package com.xxedu.learning.modules.video.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xxedu.learning.common.entity.BaseEntity;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.video.enums.VideoSourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video")
public class Video extends BaseEntity {

    private Long contentId;
    private String title;
    private String coverUrl;
    private VideoSourceType sourceType;
    private String videoUrl;
    private String qrCodeUrl;
    private Integer duration;
    private ContentStatus status;
}

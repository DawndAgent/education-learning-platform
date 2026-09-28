package com.xxedu.learning.modules.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xxedu.learning.common.entity.BaseEntity;
import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content")
public class Content extends BaseEntity {

    private String title;
    private ContentType contentType;
    private Long categoryId;
    private String coverUrl;
    private String summary;
    private ContentStatus status;
    private Integer sort;
    private Long viewCount;
    private Long favoriteCount;
    private LocalDateTime publishTime;
    private LocalDateTime scheduledPublishTime;
}

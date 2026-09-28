package com.xxedu.learning.modules.content.vo;

import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ContentDetailVO {

    private Long id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String categoryName;
}

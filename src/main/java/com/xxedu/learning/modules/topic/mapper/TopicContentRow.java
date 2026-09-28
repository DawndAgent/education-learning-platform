package com.xxedu.learning.modules.topic.mapper;

import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TopicContentRow {

    private Long itemId;
    private Long contentId;
    private LocalDateTime publishTime;
    private String title;
    private ContentType contentType;
    private String coverUrl;
    private String summary;
    private Long categoryId;
    private ContentStatus status;
    private Integer sort;
}

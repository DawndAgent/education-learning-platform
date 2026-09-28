package com.xxedu.learning.modules.article.vo;

import com.xxedu.learning.modules.content.enums.ContentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ArticleDetailVO {

    private Long contentId;
    private String title;
    private Long categoryId;
    private String coverUrl;
    private String summary;
    private ContentStatus status;
    private Integer sort;
    private LocalDateTime publishTime;
    private LocalDateTime scheduledPublishTime;
    private String body;
    private String author;
    private String source;
}

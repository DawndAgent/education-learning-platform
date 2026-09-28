package com.xxedu.learning.modules.topic.vo;

import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicContentItemVO {

    private Long id;
    private Long contentId;
    private String title;
    private ContentType contentType;
    private String coverUrl;
    private Long categoryId;
    private String categoryName;
    private ContentStatus status;
    private Integer sort;
}

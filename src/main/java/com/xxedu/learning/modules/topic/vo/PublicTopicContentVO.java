package com.xxedu.learning.modules.topic.vo;

import com.xxedu.learning.modules.content.enums.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PublicTopicContentVO {

    private Long id;
    private String title;
    private ContentType contentType;
    private String coverUrl;
    private String summary;
    private LocalDateTime publishTime;
}

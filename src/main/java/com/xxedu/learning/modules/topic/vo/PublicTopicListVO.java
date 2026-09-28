package com.xxedu.learning.modules.topic.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PublicTopicListVO {

    private Long id;
    private String name;
    private String coverUrl;
    private String summary;
    private Long categoryId;
    private String categoryName;
    private Integer sort;
    private LocalDateTime publishTime;
}

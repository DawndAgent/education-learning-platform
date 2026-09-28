package com.xxedu.learning.modules.topic.vo;

import com.xxedu.learning.modules.topic.enums.TopicStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TopicListVO {

    private Long id;
    private String name;
    private String code;
    private String coverUrl;
    private String summary;
    private Long categoryId;
    private String categoryName;
    private Integer contentCount;
    private TopicStatus status;
    private Integer sort;
    private LocalDateTime publishTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

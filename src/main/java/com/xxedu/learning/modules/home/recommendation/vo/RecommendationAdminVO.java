package com.xxedu.learning.modules.home.recommendation.vo;

import com.xxedu.learning.modules.content.enums.ContentStatus;
import com.xxedu.learning.modules.content.enums.ContentType;
import com.xxedu.learning.modules.home.enums.HomeItemStatus;
import com.xxedu.learning.modules.home.enums.RecommendType;
import com.xxedu.learning.modules.topic.enums.TopicStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendationAdminVO {

    private Long id;
    private RecommendType recommendType;
    private Long targetId;
    private String title;
    private String coverUrl;
    private String categoryName;
    private ContentType contentType;
    private ContentStatus contentStatus;
    private TopicStatus topicStatus;
    private Integer sort;
    private HomeItemStatus status;
}

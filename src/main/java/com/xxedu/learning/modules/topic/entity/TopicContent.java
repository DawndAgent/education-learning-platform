package com.xxedu.learning.modules.topic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xxedu.learning.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("topic_content")
public class TopicContent extends BaseEntity {

    private Long topicId;
    private Long contentId;
    private Integer sort;
}

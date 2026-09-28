package com.xxedu.learning.modules.topic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xxedu.learning.common.entity.BaseEntity;
import com.xxedu.learning.modules.topic.enums.TopicStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("topic")
public class Topic extends BaseEntity {

    private String name;
    private String code;
    private String coverUrl;
    private String summary;
    private Long categoryId;
    private TopicStatus status;
    private Integer sort;
    private LocalDateTime publishTime;
}

package com.xxedu.learning.modules.topic.dto;

import com.xxedu.learning.common.api.PageQuery;
import com.xxedu.learning.modules.topic.enums.TopicStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TopicQueryRequest extends PageQuery {

    private Long categoryId;
    private TopicStatus status;

    @Size(max = 64, message = "专题关键字长度不能超过64")
    private String keyword;
}

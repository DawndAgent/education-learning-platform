package com.xxedu.learning.modules.topic.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TopicContentAddRequest {

    @NotEmpty(message = "内容 ID 不能为空")
    @Size(max = 100, message = "单次最多添加 100 条内容")
    private List<Long> contentIds;
}

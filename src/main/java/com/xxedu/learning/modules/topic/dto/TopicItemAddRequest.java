package com.xxedu.learning.modules.topic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopicItemAddRequest {

    @NotNull(message = "内容 ID 不能为空")
    private Long contentId;

    @Min(value = 0, message = "排序不能小于0")
    @Max(value = 9999, message = "排序不能大于9999")
    private Integer sort;
}

package com.xxedu.learning.modules.topic.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TopicContentSortRequest {

    @NotEmpty(message = "排序项不能为空")
    @Size(max = 100, message = "单次最多排序 100 条")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "内容 ID 不能为空")
        private Long contentId;

        @NotNull(message = "排序不能为空")
        @Min(value = 0, message = "排序不能小于0")
        @Max(value = 9999, message = "排序不能大于9999")
        private Integer sort;
    }
}

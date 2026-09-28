package com.xxedu.learning.modules.topic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TopicUpdateRequest {

    @NotBlank(message = "专题名称不能为空")
    @Size(max = 200, message = "专题名称长度不能超过200")
    private String name;

    @Size(max = 500, message = "封面地址长度不能超过500")
    private String coverUrl;

    @Size(max = 500, message = "简介长度不能超过500")
    private String summary;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序不能小于0")
    @Max(value = 9999, message = "排序不能大于9999")
    private Integer sort;
}

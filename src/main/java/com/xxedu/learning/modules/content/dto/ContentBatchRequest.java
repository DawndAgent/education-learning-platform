package com.xxedu.learning.modules.content.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ContentBatchRequest {

    @NotEmpty(message = "内容 ID 不能为空")
    @Size(max = 100, message = "单次最多操作 100 条")
    private List<Long> ids;
}

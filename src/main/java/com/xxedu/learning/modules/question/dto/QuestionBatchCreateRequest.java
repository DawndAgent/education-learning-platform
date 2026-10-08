package com.xxedu.learning.modules.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class QuestionBatchCreateRequest {

    @NotEmpty(message = "请至少选择一道题目")
    @Size(max = 50, message = "单次最多导入 50 道题")
    @Valid
    private List<QuestionCreateRequest> items;
}

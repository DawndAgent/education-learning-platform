package com.xxedu.learning.modules.question.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.question.service.QuestionService;
import com.xxedu.learning.modules.question.vo.QuestionDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "题目")
@RestController
@RequestMapping(ApiConstants.PUBLIC_QUESTIONS)
@RequiredArgsConstructor
public class QuestionPublicController {

    private final QuestionService questionService;

    @Operation(summary = "已发布题目详情")
    @GetMapping("/{contentId}")
    public ApiResponse<QuestionDetailVO> detail(@PathVariable Long contentId) {
        return ApiResponse.ok(questionService.publicDetail(contentId));
    }
}

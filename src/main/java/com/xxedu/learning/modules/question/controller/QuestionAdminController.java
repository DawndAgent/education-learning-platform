package com.xxedu.learning.modules.question.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.question.dto.QuestionCreateRequest;
import com.xxedu.learning.modules.question.dto.QuestionUpdateRequest;
import com.xxedu.learning.modules.question.service.QuestionService;
import com.xxedu.learning.modules.question.vo.QuestionDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "题目管理")
@RestController
@RequestMapping(ApiConstants.ADMIN_QUESTIONS)
@RequiredArgsConstructor
public class QuestionAdminController {

    private final QuestionService questionService;

    @Operation(summary = "新增题目")
    @PostMapping
    public ApiResponse<QuestionDetailVO> create(@Valid @RequestBody QuestionCreateRequest request) {
        return ApiResponse.ok(questionService.create(request));
    }

    @Operation(summary = "修改题目")
    @PutMapping("/{contentId}")
    public ApiResponse<QuestionDetailVO> update(@PathVariable Long contentId,
                                                @Valid @RequestBody QuestionUpdateRequest request) {
        return ApiResponse.ok(questionService.update(contentId, request));
    }

    @Operation(summary = "题目详情")
    @GetMapping("/{contentId}")
    public ApiResponse<QuestionDetailVO> detail(@PathVariable Long contentId) {
        return ApiResponse.ok(questionService.adminDetail(contentId));
    }
}

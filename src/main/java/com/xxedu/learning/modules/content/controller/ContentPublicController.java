package com.xxedu.learning.modules.content.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.api.PageResult;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.content.dto.ContentQueryRequest;
import com.xxedu.learning.modules.content.service.ContentService;
import com.xxedu.learning.modules.content.vo.ContentDetailVO;
import com.xxedu.learning.modules.content.vo.ContentListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "内容")
@RestController
@RequestMapping(ApiConstants.PUBLIC_CONTENT)
@RequiredArgsConstructor
public class ContentPublicController {

    private final ContentService contentService;

    @Operation(summary = "已发布内容列表")
    @GetMapping
    public ApiResponse<PageResult<ContentListVO>> page(@Valid ContentQueryRequest request) {
        return ApiResponse.ok(contentService.publicPage(request));
    }

    @Operation(summary = "已发布内容详情")
    @GetMapping("/{id}")
    public ApiResponse<ContentDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(contentService.publicDetail(id));
    }

    @Operation(summary = "记录内容浏览")
    @PostMapping("/{id}/view")
    public ApiResponse<Void> view(@PathVariable Long id) {
        contentService.recordView(id);
        return ApiResponse.ok(null);
    }
}

package com.xxedu.learning.modules.article.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.article.dto.ArticleCreateRequest;
import com.xxedu.learning.modules.article.dto.ArticleUpdateRequest;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.article.vo.ArticleDetailVO;
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

@Tag(name = "文章管理")
@RestController
@RequestMapping(ApiConstants.ADMIN_ARTICLES)
@RequiredArgsConstructor
public class ArticleAdminController {

    private final ArticleService articleService;

    @Operation(summary = "新增文章")
    @PostMapping
    public ApiResponse<ArticleDetailVO> create(@Valid @RequestBody ArticleCreateRequest request) {
        return ApiResponse.ok(articleService.create(request));
    }

    @Operation(summary = "修改文章")
    @PutMapping("/{contentId}")
    public ApiResponse<ArticleDetailVO> update(@PathVariable Long contentId,
                                               @Valid @RequestBody ArticleUpdateRequest request) {
        return ApiResponse.ok(articleService.update(contentId, request));
    }

    @Operation(summary = "文章详情")
    @GetMapping("/{contentId}")
    public ApiResponse<ArticleDetailVO> detail(@PathVariable Long contentId) {
        return ApiResponse.ok(articleService.adminDetail(contentId));
    }
}

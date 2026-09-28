package com.xxedu.learning.modules.article.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.article.service.ArticleService;
import com.xxedu.learning.modules.article.vo.ArticleDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "文章")
@RestController
@RequestMapping(ApiConstants.PUBLIC_ARTICLES)
@RequiredArgsConstructor
public class ArticlePublicController {

    private final ArticleService articleService;

    @Operation(summary = "已发布文章详情")
    @GetMapping("/{contentId}")
    public ApiResponse<ArticleDetailVO> detail(@PathVariable Long contentId) {
        return ApiResponse.ok(articleService.publicDetail(contentId));
    }
}

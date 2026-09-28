package com.xxedu.learning.modules.home.recommendation.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.api.PageResult;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.home.dto.HomeSortRequest;
import com.xxedu.learning.modules.home.recommendation.dto.RecommendationCreateRequest;
import com.xxedu.learning.modules.home.recommendation.dto.RecommendationQueryRequest;
import com.xxedu.learning.modules.home.recommendation.service.HomeRecommendationService;
import com.xxedu.learning.modules.home.recommendation.vo.RecommendationAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "首页推荐")
@RestController
@RequestMapping(ApiConstants.ADMIN_HOME_RECOMMENDATIONS)
@RequiredArgsConstructor
public class HomeRecommendationAdminController {

    private final HomeRecommendationService homeRecommendationService;

    @Operation(summary = "推荐列表")
    @GetMapping
    public ApiResponse<PageResult<RecommendationAdminVO>> page(@Valid RecommendationQueryRequest request) {
        return ApiResponse.ok(homeRecommendationService.page(request));
    }

    @Operation(summary = "添加推荐")
    @PostMapping
    public ApiResponse<RecommendationAdminVO> create(@Valid @RequestBody RecommendationCreateRequest request) {
        return ApiResponse.ok(homeRecommendationService.create(request));
    }

    @Operation(summary = "推荐排序")
    @PutMapping("/sort")
    public ApiResponse<Void> sort(@Valid @RequestBody HomeSortRequest request) {
        homeRecommendationService.sort(request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "移除推荐")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        homeRecommendationService.delete(id);
        return ApiResponse.ok(null);
    }
}

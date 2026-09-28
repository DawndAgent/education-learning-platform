package com.xxedu.learning.modules.home.banner.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.api.PageResult;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.home.banner.dto.BannerQueryRequest;
import com.xxedu.learning.modules.home.banner.dto.BannerSaveRequest;
import com.xxedu.learning.modules.home.banner.service.HomeBannerService;
import com.xxedu.learning.modules.home.banner.vo.BannerAdminVO;
import com.xxedu.learning.modules.home.dto.HomeSortRequest;
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

@Tag(name = "首页 Banner")
@RestController
@RequestMapping(ApiConstants.ADMIN_HOME_BANNERS)
@RequiredArgsConstructor
public class HomeBannerAdminController {

    private final HomeBannerService homeBannerService;

    @Operation(summary = "Banner 列表")
    @GetMapping
    public ApiResponse<PageResult<BannerAdminVO>> page(@Valid BannerQueryRequest request) {
        return ApiResponse.ok(homeBannerService.page(request));
    }

    @Operation(summary = "新增 Banner")
    @PostMapping
    public ApiResponse<BannerAdminVO> create(@Valid @RequestBody BannerSaveRequest request) {
        return ApiResponse.ok(homeBannerService.create(request));
    }

    @Operation(summary = "Banner 排序")
    @PutMapping("/sort")
    public ApiResponse<Void> sort(@Valid @RequestBody HomeSortRequest request) {
        homeBannerService.sort(request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "编辑 Banner")
    @PutMapping("/{id}")
    public ApiResponse<BannerAdminVO> update(@PathVariable Long id, @Valid @RequestBody BannerSaveRequest request) {
        return ApiResponse.ok(homeBannerService.update(id, request));
    }

    @Operation(summary = "删除 Banner")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        homeBannerService.delete(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "启用 Banner")
    @PostMapping("/{id}/enable")
    public ApiResponse<BannerAdminVO> enable(@PathVariable Long id) {
        return ApiResponse.ok(homeBannerService.enable(id));
    }

    @Operation(summary = "停用 Banner")
    @PostMapping("/{id}/disable")
    public ApiResponse<BannerAdminVO> disable(@PathVariable Long id) {
        return ApiResponse.ok(homeBannerService.disable(id));
    }
}

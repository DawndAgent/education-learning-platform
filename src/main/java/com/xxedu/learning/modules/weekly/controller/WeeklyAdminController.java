package com.xxedu.learning.modules.weekly.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.weekly.dto.WeeklyCreateRequest;
import com.xxedu.learning.modules.weekly.dto.WeeklyUpdateRequest;
import com.xxedu.learning.modules.weekly.service.WeeklyService;
import com.xxedu.learning.modules.weekly.vo.WeeklyDetailVO;
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

@Tag(name = "每周一题管理")
@RestController
@RequestMapping(ApiConstants.ADMIN_WEEKLIES)
@RequiredArgsConstructor
public class WeeklyAdminController {

    private final WeeklyService weeklyService;

    @Operation(summary = "新增每周一题")
    @PostMapping
    public ApiResponse<WeeklyDetailVO> create(@Valid @RequestBody WeeklyCreateRequest request) {
        return ApiResponse.ok(weeklyService.create(request));
    }

    @Operation(summary = "修改每周一题")
    @PutMapping("/{contentId}")
    public ApiResponse<WeeklyDetailVO> update(@PathVariable Long contentId,
                                              @Valid @RequestBody WeeklyUpdateRequest request) {
        return ApiResponse.ok(weeklyService.update(contentId, request));
    }

    @Operation(summary = "每周一题详情")
    @GetMapping("/{contentId}")
    public ApiResponse<WeeklyDetailVO> detail(@PathVariable Long contentId) {
        return ApiResponse.ok(weeklyService.adminDetail(contentId));
    }
}

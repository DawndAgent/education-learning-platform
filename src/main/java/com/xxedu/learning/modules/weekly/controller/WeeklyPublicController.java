package com.xxedu.learning.modules.weekly.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.weekly.service.WeeklyService;
import com.xxedu.learning.modules.weekly.vo.WeeklyDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "每周一题")
@RestController
@RequestMapping(ApiConstants.PUBLIC_WEEKLIES)
@RequiredArgsConstructor
public class WeeklyPublicController {

    private final WeeklyService weeklyService;

    @Operation(summary = "已发布每周一题详情")
    @GetMapping("/{contentId}")
    public ApiResponse<WeeklyDetailVO> detail(@PathVariable Long contentId) {
        return ApiResponse.ok(weeklyService.publicDetail(contentId));
    }
}

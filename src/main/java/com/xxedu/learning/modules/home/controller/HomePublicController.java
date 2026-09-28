package com.xxedu.learning.modules.home.controller;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.modules.home.service.HomeQueryService;
import com.xxedu.learning.modules.home.vo.HomePageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "首页")
@RestController
@RequestMapping(ApiConstants.PUBLIC_HOME)
@RequiredArgsConstructor
public class HomePublicController {

    private final HomeQueryService homeQueryService;

    @Operation(summary = "首页聚合")
    @GetMapping
    public ApiResponse<HomePageVO> home() {
        return ApiResponse.ok(homeQueryService.load(LocalDateTime.now()));
    }
}

package com.xxedu.learning.health;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "基础设施")
@RestController
@RequestMapping(ApiConstants.HEALTH)
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @Operation(summary = "健康检查")
    @GetMapping
    public ApiResponse<HealthVO> health() {
        return ApiResponse.ok(healthService.current());
    }
}

package com.xxedu.learning.support;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.constant.ApiConstants;
import com.xxedu.learning.common.exception.BusinessException;
import com.xxedu.learning.common.exception.ErrorCode;
import com.xxedu.learning.security.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 只在 test profile 下注册，用于验证异常与权限框架，不会进入生产包。
 */
@Profile("test")
@RestController
public class FrameworkProbeController {

    public static final String WRITE_PERMISSION = "system:framework:write";

    @GetMapping(ApiConstants.PUBLIC + "/probe/biz-error")
    public ApiResponse<Void> bizError() {
        throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
    }

    @GetMapping(ApiConstants.PUBLIC + "/probe/boom")
    public ApiResponse<Void> boom() {
        throw new IllegalStateException("secret-token");
    }

    @PostMapping(ApiConstants.PUBLIC + "/probe/validate")
    public ApiResponse<String> validate(@Valid @RequestBody ProbeNameRequest request) {
        return ApiResponse.ok(request.getName());
    }

    @PostMapping(ApiConstants.ADMIN + "/probe/write")
    @RequirePermission(WRITE_PERMISSION)
    public ApiResponse<String> write() {
        return ApiResponse.ok("accepted");
    }

    @Getter
    @Setter
    public static class ProbeNameRequest {

        @NotBlank(message = "名称不能为空")
        private String name;
    }
}

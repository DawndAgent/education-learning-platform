package com.xxedu.learning.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        RestAuthenticationEntryPoint.write(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                ApiResponse.fail(ErrorCode.FORBIDDEN),
                objectMapper);
    }
}

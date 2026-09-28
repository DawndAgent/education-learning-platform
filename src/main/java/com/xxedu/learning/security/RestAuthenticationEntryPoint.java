package com.xxedu.learning.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, ApiResponse.fail(ErrorCode.UNAUTHORIZED));
    }

    static void write(HttpServletResponse response, int status, ApiResponse<?> body, ObjectMapper objectMapper)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private void write(HttpServletResponse response, int status, ApiResponse<?> body) throws IOException {
        write(response, status, body, objectMapper);
    }
}

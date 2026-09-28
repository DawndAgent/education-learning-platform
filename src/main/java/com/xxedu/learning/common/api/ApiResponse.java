package com.xxedu.learning.common.api;

import com.xxedu.learning.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return fail(errorCode, message, null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message, T data) {
        String resolvedMessage = message == null || message.isBlank() ? errorCode.getMessage() : message;
        return new ApiResponse<>(errorCode.getCode(), resolvedMessage, data);
    }
}

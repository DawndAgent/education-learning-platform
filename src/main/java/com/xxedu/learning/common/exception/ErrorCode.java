package com.xxedu.learning.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    SUCCESS("0", HttpStatus.OK, "success"),
    BAD_REQUEST("400", HttpStatus.BAD_REQUEST, "请求参数错误"),
    UNAUTHORIZED("401", HttpStatus.UNAUTHORIZED, "未登录或登录已失效"),
    FORBIDDEN("403", HttpStatus.FORBIDDEN, "没有权限"),
    NOT_FOUND("404", HttpStatus.NOT_FOUND, "资源不存在"),
    METHOD_NOT_ALLOWED("405", HttpStatus.METHOD_NOT_ALLOWED, "请求方法不支持"),
    INTERNAL_ERROR("500", HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后重试");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}

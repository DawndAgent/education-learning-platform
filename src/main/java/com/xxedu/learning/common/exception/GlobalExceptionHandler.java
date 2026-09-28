package com.xxedu.learning.common.exception;

import com.xxedu.learning.common.api.ApiResponse;
import com.xxedu.learning.common.api.FieldErrorVO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("business exception code={} message={}", ex.getErrorCode().getCode(), ex.getMessage());
        return status(ex.getErrorCode(), ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorVO>>> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorVO> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(new FieldErrorVO(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        for (ObjectError objectError : ex.getBindingResult().getGlobalErrors()) {
            errors.add(new FieldErrorVO(objectError.getObjectName(), objectError.getDefaultMessage()));
        }
        return validation(errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorVO>>> handleMethodValidation(HandlerMethodValidationException ex) {
        List<FieldErrorVO> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorVO(result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return validation(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorVO>>> handleConstraint(ConstraintViolationException ex) {
        List<FieldErrorVO> errors = ex.getConstraintViolations().stream()
                .map(this::toFieldError)
                .toList();
        return validation(errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorVO>>> handleMissingParam(MissingServletRequestParameterException ex) {
        return validation(List.of(new FieldErrorVO(ex.getParameterName(), "缺少必填参数")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("unreadable request");
        return status(ErrorCode.BAD_REQUEST, ApiResponse.fail(ErrorCode.BAD_REQUEST, "请求体无法解析"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        log.warn("upload too large");
        return status(ErrorCode.BAD_REQUEST, ApiResponse.fail(ErrorCode.BAD_REQUEST, "文件大小不能超过10MB"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return status(ErrorCode.METHOD_NOT_ALLOWED, ApiResponse.fail(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex) {
        log.warn("resource not found");
        return status(ErrorCode.NOT_FOUND, ApiResponse.fail(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("unhandled exception", ex);
        return status(ErrorCode.INTERNAL_ERROR, ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
    }

    private FieldErrorVO toFieldError(ConstraintViolation<?> violation) {
        return new FieldErrorVO(violation.getPropertyPath().toString(), violation.getMessage());
    }

    private ResponseEntity<ApiResponse<List<FieldErrorVO>>> validation(List<FieldErrorVO> errors) {
        log.warn("validation failed fields={}", errors.stream().map(FieldErrorVO::getField).toList());
        return status(ErrorCode.BAD_REQUEST, ApiResponse.fail(ErrorCode.BAD_REQUEST, ErrorCode.BAD_REQUEST.getMessage(), errors));
    }

    private <T> ResponseEntity<ApiResponse<T>> status(ErrorCode errorCode, ApiResponse<T> body) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }
}

package com.bike.openapi.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理：接入层错误返回真实 4xx，业务结果统一 200 + code。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResult<Object>> handleBiz(BizException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResult.fail(errorCode, exception.getMessage(), exception.getData()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return ResponseEntity.ok(ApiResult.fail(ErrorCode.PARAM_INVALID, resolveFieldMessage(exception), null));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResult<Object>> handleBind(BindException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? ErrorCode.PARAM_INVALID.getMessage() : fieldError.getDefaultMessage();
        return ResponseEntity.ok(ApiResult.fail(ErrorCode.PARAM_INVALID, message, null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Object>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(ErrorCode.PARAM_INVALID.getMessage());
        return ResponseEntity.ok(ApiResult.fail(ErrorCode.PARAM_INVALID, message, null));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResult<Object>> handleBadRequest(Exception exception) {
        return ResponseEntity.ok(ApiResult.fail(ErrorCode.PARAM_INVALID, "请求参数格式不正确", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Object>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ApiResult.fail(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Object>> handleUnexpected(Exception exception) {
        log.error("未预期的异常", exception);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResult.fail(ErrorCode.INTERNAL_ERROR));
    }

    private String resolveFieldMessage(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        return fieldError == null ? ErrorCode.PARAM_INVALID.getMessage() : fieldError.getDefaultMessage();
    }
}

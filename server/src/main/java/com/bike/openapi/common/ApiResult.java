package com.bike.openapi.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.MDC;

/**
 * 统一响应体。
 */
@Getter
@Setter
public class ApiResult<T> {

    /** 成功码。 */
    public static final String SUCCESS_CODE = "0";

    private String code;
    private String message;
    @Schema(description = "业务数据，失败或无返回时为 null")
    private T data;
    private String traceId;

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(SUCCESS_CODE);
        result.setMessage("success");
        result.setData(data);
        result.setTraceId(MDC.get(TraceIdFilter.TRACE_ID));
        return result;
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode, String message, T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(errorCode.getCode());
        result.setMessage(message == null ? errorCode.getMessage() : message);
        result.setData(data);
        result.setTraceId(MDC.get(TraceIdFilter.TRACE_ID));
        return result;
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage(), null);
    }
}

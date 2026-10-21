package com.bike.openapi.common;

/**
 * 分层错误码：GW- 为接入层错误，BIZ- 为业务结果。
 */
public enum ErrorCode {

    MISSING_HEADER("GW-40001", 400, "缺少必要请求头"),
    APP_INVALID("GW-40002", 401, "AppKey 无效或已禁用"),
    SIGNATURE_INVALID("GW-40003", 401, "签名校验失败"),
    TIMESTAMP_INVALID("GW-40004", 400, "时间戳超出允许范围"),
    NONCE_REPEATED("GW-40005", 400, "Nonce 重复，疑似重放请求"),
    IP_FORBIDDEN("GW-40006", 403, "IP 不在白名单内"),
    RATE_LIMITED("GW-40007", 429, "请求过于频繁"),
    IDEMPOTENCY_KEY_MISSING("GW-40008", 400, "写接口缺少 Idempotency-Key"),
    REDIS_UNAVAILABLE("GW-50001", 503, "服务暂不可用，请稍后重试"),
    INTERNAL_ERROR("GW-50000", 500, "服务内部错误"),
    PARAM_INVALID("BIZ-40001", 200, "参数校验失败"),
    ORDER_EXISTS("BIZ-40901", 200, "订单已存在"),
    UNAUTHORIZED("ADM-40100", 401, "未登录或登录已过期"),
    CAPTCHA_INVALID("ADM-40001", 400, "验证码错误或已过期"),
    FORBIDDEN("ADM-40300", 403, "无操作权限"),
    PASSWORD_CHANGE_REQUIRED("ADM-40301", 403, "请先修改初始密码");

    private final String code;
    private final int httpStatus;
    private final String message;

    ErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}

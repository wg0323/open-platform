package com.bike.openapi.metrics;

import lombok.RequiredArgsConstructor;
import com.bike.openapi.common.ApiResult;
import com.bike.openapi.security.ApiAuthFilter;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 在响应序列化前记录耗时与响应码，覆盖正常返回与业务异常。
 */
@ControllerAdvice
@RequiredArgsConstructor
public class ApiMetricsAdvice implements ResponseBodyAdvice<Object> {

    private final ApiMetricsRecorder apiMetricsRecorder;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest) || !(body instanceof ApiResult)) {
            return body;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        Object appKey = servletRequest.getAttribute(ApiAuthFilter.ATTR_APP_KEY);
        if (appKey == null) {
            return body;
        }
        Object startTime = servletRequest.getAttribute(ApiAuthFilter.ATTR_START_TIME);
        long cost = startTime instanceof Long ? System.currentTimeMillis() - (Long) startTime : 0L;
        apiMetricsRecorder.record(appKey.toString(), cost, ((ApiResult<?>) body).getCode(), false);
        return body;
    }
}

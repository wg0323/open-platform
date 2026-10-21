package com.bike.openapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import com.bike.openapi.common.ApiResult;
import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;
import com.bike.openapi.config.OpenApiProperties;
import com.bike.openapi.domain.AppInfo;
import com.bike.openapi.metrics.ApiMetricsRecorder;
import com.bike.openapi.ratelimit.RedisRateLimiter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * 接入鉴权链：查凭证、IP 白名单、IP 粗限流、时间戳、验签、Nonce、appKey 限流。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiAuthFilter extends OncePerRequestFilter {

    /** request 属性：appKey。 */
    public static final String ATTR_APP_KEY = "openapi.appKey";
    /** request 属性：请求开始时间。 */
    public static final String ATTR_START_TIME = "openapi.startTime";

    private static final String HEADER_APP_KEY = "X-App-Key";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGNATURE = "X-Signature";
    /** 幂等键请求头。 */
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final String API_PREFIX = "/api/v3/";
    private static final String UNKNOWN_APP_KEY = "unknown";
    private static final long SECOND_MILLIS = 1000L;
    private static final long MILLIS_THRESHOLD = 1_000_000_000_000L;
    private static final String RATE_LIMIT_APP_PREFIX = "rl:app:";
    private static final String RATE_LIMIT_IP_PREFIX = "rl:ip:";

    /** 需要强制携带幂等键的写接口。 */
    private static final Set<String> IDEMPOTENT_PATHS = Collections.singleton("/api/v3/orders");

    private final AppCredentialService appCredentialService;
    private final NonceService nonceService;
    private final RedisRateLimiter rateLimiter;
    private final ApiMetricsRecorder metricsRecorder;
    private final OpenApiProperties properties;
    private final ObjectMapper objectMapper;

    public ApiAuthFilter(AppCredentialService appCredentialService, NonceService nonceService,
                         RedisRateLimiter rateLimiter, ApiMetricsRecorder metricsRecorder,
                         OpenApiProperties properties, ObjectMapper objectMapper) {
        this.appCredentialService = appCredentialService;
        this.nonceService = nonceService;
        this.rateLimiter = rateLimiter;
        this.metricsRecorder = metricsRecorder;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(API_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        long start = System.currentTimeMillis();
        String appKey = null;
        try {
            CachedBodyHttpServletRequest cachedRequest =
                    new CachedBodyHttpServletRequest(request, properties.getSecurity().getMaxBodyBytes());
            appKey = requireHeader(cachedRequest, HEADER_APP_KEY);
            String timestamp = requireHeader(cachedRequest, HEADER_TIMESTAMP);
            String nonce = requireHeader(cachedRequest, HEADER_NONCE);
            String signature = requireHeader(cachedRequest, HEADER_SIGNATURE);

            AppInfo appInfo = loadAppInfo(appKey);
            checkIpWhitelist(cachedRequest, appInfo);
            checkIpRateLimit(cachedRequest);
            checkTimestamp(timestamp);
            checkSignature(cachedRequest, appInfo, timestamp, nonce, signature);
            checkNonce(appKey, nonce);
            checkAppRateLimit(appInfo);
            checkIdempotencyKey(cachedRequest);

            cachedRequest.setAttribute(ATTR_APP_KEY, appKey);
            cachedRequest.setAttribute(ATTR_START_TIME, start);
            chain.doFilter(cachedRequest, response);
        } catch (BizException exception) {
            metricsRecorder.record(appKey == null ? UNKNOWN_APP_KEY : appKey,
                    System.currentTimeMillis() - start, exception.getErrorCode().getCode(),
                    exception.getErrorCode() == ErrorCode.RATE_LIMITED);
            writeError(response, exception.getErrorCode(), exception.getMessage());
        }
    }

    private String requireHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.trim().isEmpty()) {
            throw new BizException(ErrorCode.MISSING_HEADER, "缺少请求头 " + name);
        }
        return value.trim();
    }

    private AppInfo loadAppInfo(String appKey) {
        Optional<AppInfo> appInfo = appCredentialService.find(appKey);
        if (!appInfo.isPresent()) {
            throw new BizException(ErrorCode.APP_INVALID);
        }
        if (!Boolean.TRUE.equals(appInfo.get().getEnabled())) {
            throw new BizException(ErrorCode.APP_INVALID, "AppKey 已禁用");
        }
        return appInfo.get();
    }

    private void checkIpWhitelist(HttpServletRequest request, AppInfo appInfo) {
        if (!properties.getSecurity().isIpWhitelistEnabled()) {
            return;
        }
        String clientIp = request.getRemoteAddr();
        if (!IpWhitelistChecker.matches(clientIp, appInfo.getIpWhitelist())) {
            log.warn("IP 不在白名单 appKey={} ip={}", appInfo.getAppKey(), clientIp);
            throw new BizException(ErrorCode.IP_FORBIDDEN);
        }
    }

    private void checkIpRateLimit(HttpServletRequest request) {
        if (!properties.getRateLimit().isEnabled()) {
            return;
        }
        String key = RATE_LIMIT_IP_PREFIX + request.getRemoteAddr();
        if (!rateLimiter.tryAcquire(key, properties.getRateLimit().getIpQps())) {
            throw new BizException(ErrorCode.RATE_LIMITED);
        }
    }

    private void checkTimestamp(String timestamp) {
        long requestMillis;
        try {
            requestMillis = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.TIMESTAMP_INVALID, "时间戳格式不正确");
        }
        if (requestMillis < MILLIS_THRESHOLD) {
            requestMillis = requestMillis * SECOND_MILLIS;
        }
        long windowMillis = properties.getSecurity().getTimestampWindowSeconds() * SECOND_MILLIS;
        if (Math.abs(System.currentTimeMillis() - requestMillis) > windowMillis) {
            throw new BizException(ErrorCode.TIMESTAMP_INVALID);
        }
    }

    private void checkSignature(HttpServletRequest request, AppInfo appInfo,
                                String timestamp, String nonce, String signature) {
        String canonical = SignatureUtil.canonicalString(request.getMethod(), request.getRequestURI(),
                request.getQueryString(), timestamp, nonce, readBody(request));
        if (!SignatureUtil.verify(appInfo.getAppSecret(), canonical, signature)) {
            log.warn("签名校验失败 appKey={} path={}", appInfo.getAppKey(), request.getRequestURI());
            throw new BizException(ErrorCode.SIGNATURE_INVALID);
        }
    }

    private byte[] readBody(HttpServletRequest request) {
        if (request instanceof CachedBodyHttpServletRequest) {
            return ((CachedBodyHttpServletRequest) request).getBody();
        }
        return new byte[0];
    }

    private void checkNonce(String appKey, String nonce) {
        if (!nonceService.markIfAbsent(appKey, nonce, properties.getSecurity().getNonceTtlSeconds())) {
            throw new BizException(ErrorCode.NONCE_REPEATED);
        }
    }

    private void checkAppRateLimit(AppInfo appInfo) {
        if (!properties.getRateLimit().isEnabled()) {
            return;
        }
        String key = RATE_LIMIT_APP_PREFIX + appInfo.getAppKey();
        if (!rateLimiter.tryAcquire(key, appInfo.getQps() == null ? 0 : appInfo.getQps())) {
            throw new BizException(ErrorCode.RATE_LIMITED);
        }
    }

    private void checkIdempotencyKey(HttpServletRequest request) {
        if (!IDEMPOTENT_PATHS.contains(request.getRequestURI())) {
            return;
        }
        String key = request.getHeader(HEADER_IDEMPOTENCY_KEY);
        if (key == null || key.trim().isEmpty()) {
            throw new BizException(ErrorCode.IDEMPOTENCY_KEY_MISSING);
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(errorCode, message, null)));
    }
}

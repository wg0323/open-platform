package com.bike.openapi.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档与指标注册配置。
 */
@Configuration
public class OpenApiDocConfig {

    private static final double PERCENTILE = 0.99D;

    private static final String SCHEME_APP_KEY = "AppKey";
    private static final String SCHEME_TIMESTAMP = "Timestamp";
    private static final String SCHEME_NONCE = "Nonce";
    private static final String SCHEME_SIGNATURE = "Signature";
    private static final String HEADER_APP_KEY = "X-App-Key";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGNATURE = "X-Signature";

    private static final String API_DESCRIPTION =
            "## 接口清单\n"
            + "本服务对外开放四个接口：`GET /api/v3/fences`、`GET /api/v3/areas`、"
            + "`POST /api/v3/orders`（必须携带 `Idempotency-Key`）、`POST /api/v3/positions`"
            + "（按 appKey + sn + positionTime 幂等覆盖）。\n\n"
            + "## 鉴权\n"
            + "所有 `/api/v3` 接口均需在请求头携带 `X-App-Key`、`X-Timestamp`（毫秒或秒级，允许 ±300 秒）、"
            + "`X-Nonce`（同一 appKey 下 10 分钟内不可重复）、`X-Signature`。\n\n"
            + "待签串由六段组成，用单个换行（LF）连接：HTTP 方法、请求路径、规范化 query"
            + "（按参数名升序排序，不做 URL 解码，无参数时为空串）、timestamp、nonce、"
            + "body 的 SHA-256 后 Base64；`X-Signature = Base64(HMAC-SHA256(待签串, appSecret))`。\n\n"
            + "空 body 的 SHA-256 固定为 `47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=`。\n\n"
            + "## 时间格式\n"
            + "业务时间字段统一为 `yyyy-MM-dd'T'HH:mm:ss`，精确到秒，不带时区后缀，按北京时间解释。\n\n"
            + "## 响应与错误码\n"
            + "统一响应 `{code, message, data, traceId}`，`code` 为 `\"0\"` 表示成功。\n\n"
            + "接入层错误返回真实 HTTP 状态码：`GW-40001` 缺少请求头 400、`GW-40002` AppKey 无效或禁用 401、"
            + "`GW-40003` 签名校验失败 401、`GW-40004` 时间戳超出范围 400、`GW-40005` Nonce 重复 400、"
            + "`GW-40006` IP 不在白名单 403、`GW-40007` 限流 429、`GW-40008` 缺少幂等键 400、"
            + "`GW-50000` 服务内部错误 500、`GW-50001` 服务暂不可用 503。\n\n"
            + "业务结果统一返回 HTTP 200：`BIZ-40001` 参数校验失败、`BIZ-40901` 订单已存在"
            + "（其 `data` 为已存在订单，应按成功处理）。";

    @Bean
    public OpenAPI openApi(@Value("${openapi.doc.server-url:}") String serverUrl) {
        OpenAPI openApi = new OpenAPI().info(new Info()
                        .title("共享自行车监管平台开放 API v3")
                        .version("3.0.0")
                        .description(API_DESCRIPTION))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_APP_KEY, apiKey(HEADER_APP_KEY, "接入方标识，由监管平台分配"))
                        .addSecuritySchemes(SCHEME_TIMESTAMP,
                                apiKey(HEADER_TIMESTAMP, "Unix 时间戳，毫秒或秒级，允许与服务器相差 ±300 秒"))
                        .addSecuritySchemes(SCHEME_NONCE,
                                apiKey(HEADER_NONCE, "每次请求唯一的随机串，同一 appKey 下 10 分钟内不可重复"))
                        .addSecuritySchemes(SCHEME_SIGNATURE,
                                apiKey(HEADER_SIGNATURE, "HMAC-SHA256 签名，标准 Base64 编码")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SCHEME_APP_KEY)
                        .addList(SCHEME_TIMESTAMP)
                        .addList(SCHEME_NONCE)
                        .addList(SCHEME_SIGNATURE));
        if (serverUrl != null && !serverUrl.trim().isEmpty()) {
            openApi.addServersItem(new Server().url(serverUrl.trim()).description("监管平台服务地址"));
        }
        return openApi;
    }

    private static SecurityScheme apiKey(String headerName, String description) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(headerName)
                .description(description);
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCustomizer() {
        return registry -> registry.config().meterFilter(new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if ("openapi.request.latency".equals(id.getName())) {
                    return DistributionStatisticConfig.builder()
                            .percentiles(PERCENTILE)
                            .percentilesHistogram(true)
                            .build()
                            .merge(config);
                }
                return config;
            }
        });
    }
}

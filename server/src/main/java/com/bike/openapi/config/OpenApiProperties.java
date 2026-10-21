package com.bike.openapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 开放 API 网关配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "openapi")
public class OpenApiProperties {

    private Security security = new Security();
    private RateLimit rateLimit = new RateLimit();
    private Metrics metrics = new Metrics();

    @Data
    public static class Security {
        /** 时间戳允许的偏移秒数。 */
        private int timestampWindowSeconds = 300;
        /** Nonce 存储的 TTL 秒数，必须大于时间窗口。 */
        private int nonceTtlSeconds = 600;
        /** 是否启用 IP 白名单。 */
        private boolean ipWhitelistEnabled = false;
        /** 请求体最大字节数。 */
        private int maxBodyBytes = 1048576;
    }

    @Data
    public static class RateLimit {
        /** 是否启用限流。 */
        private boolean enabled = true;
        /** IP 维度粗限流阈值。 */
        private int ipQps = 100;
    }

    @Data
    public static class Metrics {
        /** 指标统计窗口秒数。 */
        private int windowSeconds = 60;
        /** 延迟样本数量。 */
        private int latencySamples = 1024;
        /** 分钟级历史数据保留天数。 */
        private int retentionDays = 7;
    }
}

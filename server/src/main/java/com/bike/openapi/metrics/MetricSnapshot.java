package com.bike.openapi.metrics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 按 appKey 聚合的指标快照。
 */
@Data
@Schema(description = "按 appKey 聚合的指标")
public class MetricSnapshot {

    @Schema(description = "接入方标识")
    private String appKey;

    @Schema(description = "窗口内请求数")
    private long requests;

    @Schema(description = "窗口内错误数")
    private long errors;

    @Schema(description = "窗口内每秒请求数")
    private double qps;

    @Schema(description = "窗口内错误率，0~1")
    private double errorRate;

    @Schema(description = "窗口内限流拒绝数")
    private long limited;

    @Schema(description = "窗口内幂等命中数")
    private long idempotentHits;

    @Schema(description = "窗口内 P99 延迟，毫秒")
    private long p99Millis;
}

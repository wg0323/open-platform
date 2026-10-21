package com.bike.openapi.admin.dto;

import com.bike.openapi.domain.ApiMetricMinute;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分钟级指标历史点。
 */
@Data
@Schema(description = "分钟级指标历史点")
public class MetricHistoryPointVo {

    @Schema(description = "统计分钟")
    private LocalDateTime statMinute;

    @Schema(description = "请求数")
    private long requests;

    @Schema(description = "错误数")
    private long errors;

    @Schema(description = "错误率，0~1")
    private double errorRate;

    @Schema(description = "限流拒绝数")
    private long limited;

    @Schema(description = "幂等命中数")
    private long idempotentHits;

    @Schema(description = "P99 延迟，毫秒")
    private long p99Millis;

    /**
     * 由分钟快照构造历史点。
     *
     * @param metric 分钟快照
     * @return 历史点
     */
    public static MetricHistoryPointVo of(ApiMetricMinute metric) {
        MetricHistoryPointVo point = new MetricHistoryPointVo();
        point.setStatMinute(metric.getStatMinute());
        point.setRequests(metric.getRequests());
        point.setErrors(metric.getErrors());
        point.setErrorRate(metric.getRequests() == 0L ? 0D
                : metric.getErrors() / (double) metric.getRequests());
        point.setLimited(metric.getLimited());
        point.setIdempotentHits(metric.getIdempotentHits());
        point.setP99Millis(metric.getP99Millis());
        return point;
    }
}

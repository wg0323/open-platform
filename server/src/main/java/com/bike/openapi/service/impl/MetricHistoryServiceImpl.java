package com.bike.openapi.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;
import com.bike.openapi.config.OpenApiProperties;
import com.bike.openapi.domain.ApiMetricMinute;
import com.bike.openapi.mapper.ApiMetricMinuteMapper;
import com.bike.openapi.metrics.ApiMetricsRecorder;
import com.bike.openapi.metrics.MetricSnapshot;
import com.bike.openapi.service.MetricHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指标历史服务实现：每分钟落库窗口快照，每天清理过期数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricHistoryServiceImpl implements MetricHistoryService {

    private static final int DEFAULT_RANGE_HOURS = 1;

    private final ApiMetricsRecorder apiMetricsRecorder;
    private final ApiMetricMinuteMapper apiMetricMinuteMapper;
    private final OpenApiProperties properties;

    @Override
    @Scheduled(cron = "0 * * * * ?")
    public void flushMinute() {
        LocalDateTime statMinute = LocalDateTime.now().minusMinutes(1).withSecond(0).withNano(0);
        for (MetricSnapshot snapshot : apiMetricsRecorder.snapshotAll()) {
            apiMetricMinuteMapper.upsert(toEntity(snapshot, statMinute));
        }
    }

    @Override
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        LocalDateTime expireBefore = LocalDateTime.now().minusDays(retentionDays());
        int deleted = apiMetricMinuteMapper.delete(Wrappers.<ApiMetricMinute>lambdaQuery()
                .lt(ApiMetricMinute::getStatMinute, expireBefore));
        log.info("清理过期指标快照 {} 条，截止时间 {}", deleted, expireBefore);
    }

    @Override
    public List<ApiMetricMinute> query(String appKey, LocalDateTime start, LocalDateTime end) {
        if (appKey == null || appKey.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "appKey 不能为空");
        }
        LocalDateTime queryEnd = end == null ? LocalDateTime.now() : end;
        LocalDateTime queryStart = start == null ? queryEnd.minusHours(DEFAULT_RANGE_HOURS) : start;
        if (!queryStart.isBefore(queryEnd)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "开始时间必须早于结束时间");
        }
        if (queryStart.isBefore(queryEnd.minusDays(retentionDays()))) {
            throw new BizException(ErrorCode.PARAM_INVALID, "查询跨度不能超过 " + retentionDays() + " 天");
        }
        return apiMetricMinuteMapper.selectList(Wrappers.<ApiMetricMinute>lambdaQuery()
                .eq(ApiMetricMinute::getAppKey, appKey.trim())
                .ge(ApiMetricMinute::getStatMinute, queryStart)
                .le(ApiMetricMinute::getStatMinute, queryEnd)
                .orderByAsc(ApiMetricMinute::getStatMinute));
    }

    private ApiMetricMinute toEntity(MetricSnapshot snapshot, LocalDateTime statMinute) {
        ApiMetricMinute metric = new ApiMetricMinute();
        metric.setAppKey(snapshot.getAppKey());
        metric.setStatMinute(statMinute);
        metric.setRequests(snapshot.getRequests());
        metric.setErrors(snapshot.getErrors());
        metric.setLimited(snapshot.getLimited());
        metric.setIdempotentHits(snapshot.getIdempotentHits());
        metric.setP99Millis(snapshot.getP99Millis());
        return metric;
    }

    private int retentionDays() {
        return Math.max(1, properties.getMetrics().getRetentionDays());
    }
}

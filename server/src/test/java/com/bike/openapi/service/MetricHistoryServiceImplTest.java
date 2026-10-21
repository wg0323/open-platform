package com.bike.openapi.service;

import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;
import com.bike.openapi.config.OpenApiProperties;
import com.bike.openapi.domain.ApiMetricMinute;
import com.bike.openapi.mapper.ApiMetricMinuteMapper;
import com.bike.openapi.metrics.ApiMetricsRecorder;
import com.bike.openapi.metrics.MetricSnapshot;
import com.bike.openapi.service.impl.MetricHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 指标历史服务单元测试：落库值、查询校验与清理。
 */
class MetricHistoryServiceImplTest {

    private static final String APP_KEY = "sbike_demo";

    private ApiMetricsRecorder recorder;
    private ApiMetricMinuteMapper mapper;
    private MetricHistoryServiceImpl service;

    @BeforeEach
    void setUp() {
        recorder = mock(ApiMetricsRecorder.class);
        mapper = mock(ApiMetricMinuteMapper.class);
        service = new MetricHistoryServiceImpl(recorder, mapper, new OpenApiProperties());
    }

    @Test
    void flushShouldWritePreviousMinuteSnapshot() {
        MetricSnapshot snapshot = new MetricSnapshot();
        snapshot.setAppKey(APP_KEY);
        snapshot.setRequests(120L);
        snapshot.setErrors(6L);
        snapshot.setLimited(3L);
        snapshot.setIdempotentHits(2L);
        snapshot.setP99Millis(88L);
        when(recorder.snapshotAll()).thenReturn(Collections.singletonList(snapshot));

        service.flushMinute();

        ArgumentCaptor<ApiMetricMinute> captor = ArgumentCaptor.forClass(ApiMetricMinute.class);
        verify(mapper).upsert(captor.capture());
        ApiMetricMinute saved = captor.getValue();
        assertEquals(APP_KEY, saved.getAppKey());
        assertEquals(120L, saved.getRequests().longValue());
        assertEquals(6L, saved.getErrors().longValue());
        assertEquals(3L, saved.getLimited().longValue());
        assertEquals(2L, saved.getIdempotentHits().longValue());
        assertEquals(88L, saved.getP99Millis().longValue());
        assertEquals(0, saved.getStatMinute().getSecond());
        assertEquals(0, saved.getStatMinute().getNano());
        long gapSeconds = Math.abs(Duration.between(saved.getStatMinute(), LocalDateTime.now()).getSeconds());
        assertTrue(gapSeconds <= 120, "统计分钟应落在上一分钟，实际相差 " + gapSeconds + " 秒");
    }

    @Test
    void queryShouldRejectSpanBeyondRetention() {
        LocalDateTime end = LocalDateTime.now();

        BizException exception = assertThrows(BizException.class,
                () -> service.query(APP_KEY, end.minusDays(8), end));

        assertEquals(ErrorCode.PARAM_INVALID, exception.getErrorCode());
        verify(mapper, never()).selectList(any());
    }

    @Test
    void queryShouldRejectBlankAppKey() {
        BizException exception = assertThrows(BizException.class, () -> service.query(" ", null, null));

        assertEquals(ErrorCode.PARAM_INVALID, exception.getErrorCode());
        verify(mapper, never()).selectList(any());
    }

    @Test
    void queryShouldDefaultToOneHourAndReturnRows() {
        ApiMetricMinute row = new ApiMetricMinute();
        row.setAppKey(APP_KEY);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(row));

        List<ApiMetricMinute> rows = service.query(APP_KEY, null, null);

        assertEquals(1, rows.size());
        assertEquals(APP_KEY, rows.get(0).getAppKey());
    }

    @Test
    void cleanupShouldDeleteExpiredRows() {
        when(mapper.delete(any())).thenReturn(5);

        service.cleanup();

        verify(mapper).delete(any());
    }
}

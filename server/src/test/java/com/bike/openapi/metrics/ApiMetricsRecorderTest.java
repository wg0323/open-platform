package com.bike.openapi.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.bike.openapi.config.OpenApiProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 指标窗口统计单元测试。
 */
class ApiMetricsRecorderTest {

    private static final String APP_KEY = "sbike_demo";

    @Test
    void snapshotShouldAggregateRequestsErrorsLimitAndLatency() {
        OpenApiProperties properties = new OpenApiProperties();
        ApiMetricsRecorder recorder = new ApiMetricsRecorder(properties, new SimpleMeterRegistry());

        recorder.record(APP_KEY, 10L, "0", false);
        recorder.record(APP_KEY, 20L, "0", false);
        recorder.record(APP_KEY, 30L, "GW-40007", true);
        recorder.record(APP_KEY, 40L, "BIZ-40901", false);
        recorder.recordIdempotentHit(APP_KEY);

        List<MetricSnapshot> snapshots = recorder.snapshotAll();
        assertEquals(1, snapshots.size());

        MetricSnapshot snapshot = snapshots.get(0);
        assertEquals(APP_KEY, snapshot.getAppKey());
        assertEquals(4L, snapshot.getRequests());
        assertEquals(0.5D, snapshot.getErrorRate(), 0.0001D);
        assertEquals(1L, snapshot.getLimited());
        assertEquals(1L, snapshot.getIdempotentHits());
        assertEquals(40L, snapshot.getP99Millis());
        assertTrue(snapshot.getQps() > 0D);
    }

    @Test
    void unknownAppKeyShouldReturnEmptySnapshotList() {
        ApiMetricsRecorder recorder = new ApiMetricsRecorder(new OpenApiProperties(), new SimpleMeterRegistry());

        assertTrue(recorder.snapshotAll().isEmpty());
    }
}

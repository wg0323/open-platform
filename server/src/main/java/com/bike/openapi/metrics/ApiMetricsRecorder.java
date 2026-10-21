package com.bike.openapi.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import com.bike.openapi.config.OpenApiProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 指标采集：内存秒级窗口统计 + Micrometer 打点。
 */
@Component
public class ApiMetricsRecorder {

    private static final long MILLIS_PER_SECOND = 1000L;
    private static final double PERCENTILE = 0.99D;
    private static final String METRIC_REQUESTS = "openapi.request.total";
    private static final String METRIC_LATENCY = "openapi.request.latency";
    private static final String METRIC_LIMITED = "openapi.ratelimit.rejected";
    private static final String METRIC_IDEMPOTENT = "openapi.idempotent.hit";
    private static final String TAG_APP_KEY = "appKey";
    private static final String TAG_CODE = "code";

    private final int windowSeconds;
    private final int latencySampleSize;
    private final MeterRegistry meterRegistry;
    private final Map<String, AppMetric> metrics = new ConcurrentHashMap<>();

    public ApiMetricsRecorder(OpenApiProperties properties, MeterRegistry meterRegistry) {
        this.windowSeconds = Math.max(1, properties.getMetrics().getWindowSeconds());
        this.latencySampleSize = Math.max(16, properties.getMetrics().getLatencySamples());
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录一次请求。
     *
     * @param appKey     接入方标识
     * @param costMillis 耗时
     * @param code       响应码
     * @param limited    是否被限流拒绝
     */
    public void record(String appKey, long costMillis, String code, boolean limited) {
        boolean error = !"0".equals(code);
        metric(appKey).record(System.currentTimeMillis() / MILLIS_PER_SECOND, costMillis, error, limited);
        meterRegistry.counter(METRIC_REQUESTS, TAG_APP_KEY, appKey, TAG_CODE, code).increment();
        meterRegistry.timer(METRIC_LATENCY, TAG_APP_KEY, appKey).record(costMillis, TimeUnit.MILLISECONDS);
        if (limited) {
            meterRegistry.counter(METRIC_LIMITED, TAG_APP_KEY, appKey).increment();
        }
    }

    /**
     * 记录一次幂等命中。
     *
     * @param appKey 接入方标识
     */
    public void recordIdempotentHit(String appKey) {
        metric(appKey).recordIdempotentHit(System.currentTimeMillis() / MILLIS_PER_SECOND);
        meterRegistry.counter(METRIC_IDEMPOTENT, TAG_APP_KEY, appKey).increment();
    }

    /**
     * 汇总所有 appKey 的指标。
     *
     * @return 指标快照
     */
    public List<MetricSnapshot> snapshotAll() {
        List<MetricSnapshot> snapshots = new ArrayList<>(metrics.size());
        long nowSecond = System.currentTimeMillis() / MILLIS_PER_SECOND;
        for (Map.Entry<String, AppMetric> entry : metrics.entrySet()) {
            snapshots.add(entry.getValue().snapshot(entry.getKey(), nowSecond));
        }
        return snapshots;
    }

    private AppMetric metric(String appKey) {
        return metrics.computeIfAbsent(appKey, key -> new AppMetric(windowSeconds, latencySampleSize));
    }

    /**
     * 秒级环形窗口与延迟样本环。
     */
    private static final class AppMetric {

        private final SecondBucket[] buckets;
        private final long[] latencySamples;
        private int latencyCursor;
        private int latencyCount;

        private AppMetric(int windowSeconds, int latencySize) {
            this.buckets = new SecondBucket[windowSeconds];
            for (int index = 0; index < windowSeconds; index++) {
                buckets[index] = new SecondBucket();
            }
            this.latencySamples = new long[latencySize];
        }

        private void record(long nowSecond, long costMillis, boolean error, boolean limited) {
            SecondBucket bucket = buckets[(int) (nowSecond % buckets.length)];
            synchronized (bucket) {
                if (bucket.second != nowSecond) {
                    bucket.reset(nowSecond);
                }
                bucket.requests++;
                if (error) {
                    bucket.errors++;
                }
                if (limited) {
                    bucket.limited++;
                }
            }
            synchronized (latencySamples) {
                latencySamples[latencyCursor] = costMillis;
                latencyCursor = (latencyCursor + 1) % latencySamples.length;
                if (latencyCount < latencySamples.length) {
                    latencyCount++;
                }
            }
        }

        private void recordIdempotentHit(long nowSecond) {
            SecondBucket bucket = buckets[(int) (nowSecond % buckets.length)];
            synchronized (bucket) {
                if (bucket.second != nowSecond) {
                    bucket.reset(nowSecond);
                }
                bucket.idempotentHits++;
            }
        }

        private MetricSnapshot snapshot(String appKey, long nowSecond) {
            long windowStart = nowSecond - buckets.length + 1;
            long requests = 0;
            long errors = 0;
            long limited = 0;
            long idempotentHits = 0;
            for (SecondBucket bucket : buckets) {
                synchronized (bucket) {
                    if (bucket.second >= windowStart) {
                        requests += bucket.requests;
                        errors += bucket.errors;
                        limited += bucket.limited;
                        idempotentHits += bucket.idempotentHits;
                    }
                }
            }
            MetricSnapshot snapshot = new MetricSnapshot();
            snapshot.setAppKey(appKey);
            snapshot.setRequests(requests);
            snapshot.setErrors(errors);
            snapshot.setQps(requests / (double) buckets.length);
            snapshot.setErrorRate(requests == 0 ? 0D : errors / (double) requests);
            snapshot.setLimited(limited);
            snapshot.setIdempotentHits(idempotentHits);
            snapshot.setP99Millis(percentile99());
            return snapshot;
        }

        private long percentile99() {
            long[] copy;
            int size;
            synchronized (latencySamples) {
                size = latencyCount;
                copy = Arrays.copyOf(latencySamples, size);
            }
            if (size == 0) {
                return 0L;
            }
            Arrays.sort(copy);
            int index = (int) Math.ceil(PERCENTILE * size) - 1;
            return copy[Math.max(0, Math.min(index, size - 1))];
        }
    }

    /**
     * 单秒计数桶。
     */
    private static final class SecondBucket {

        private long second = -1L;
        private long requests;
        private long errors;
        private long limited;
        private long idempotentHits;

        private void reset(long targetSecond) {
            this.second = targetSecond;
            this.requests = 0L;
            this.errors = 0L;
            this.limited = 0L;
            this.idempotentHits = 0L;
        }
    }
}

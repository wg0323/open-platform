package com.bike.openapi.service;

import com.bike.openapi.domain.ApiMetricMinute;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指标历史服务：分钟快照落库、范围查询与过期清理。
 */
public interface MetricHistoryService {

    /**
     * 把当前内存窗口的指标写入上一分钟。
     */
    void flushMinute();

    /**
     * 删除保留期之外的分钟快照。
     */
    void cleanup();

    /**
     * 按 appKey 与时间范围查询分钟级历史，按时间升序返回。
     *
     * @param appKey 接入方标识
     * @param start  起始时间，为空时取结束时间前 1 小时
     * @param end    结束时间，为空时取当前时间
     * @return 分钟快照列表
     */
    List<ApiMetricMinute> query(String appKey, LocalDateTime start, LocalDateTime end);
}

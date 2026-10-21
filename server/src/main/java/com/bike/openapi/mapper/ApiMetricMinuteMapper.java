package com.bike.openapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bike.openapi.domain.ApiMetricMinute;
import org.apache.ibatis.annotations.Insert;

/**
 * 接口指标分钟快照 Mapper。
 */
public interface ApiMetricMinuteMapper extends BaseMapper<ApiMetricMinute> {

    /**
     * 按 appKey + 分钟覆盖写入，同一分钟重复落库不产生重复行。
     *
     * @param metric 分钟快照
     * @return 影响行数
     */
    @Insert("INSERT INTO api_metric_minute (app_key, stat_minute, requests, errors, limited, idempotent_hits, p99_millis) "
            + "VALUES (#{appKey}, #{statMinute}, #{requests}, #{errors}, #{limited}, #{idempotentHits}, #{p99Millis}) "
            + "ON DUPLICATE KEY UPDATE requests = VALUES(requests), errors = VALUES(errors), "
            + "limited = VALUES(limited), idempotent_hits = VALUES(idempotent_hits), p99_millis = VALUES(p99_millis)")
    int upsert(ApiMetricMinute metric);
}

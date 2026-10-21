package com.bike.openapi.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口指标分钟快照。
 */
@Data
@TableName("api_metric_minute")
public class ApiMetricMinute {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String appKey;
    private LocalDateTime statMinute;
    private Long requests;
    private Long errors;
    private Long limited;
    private Long idempotentHits;
    private Long p99Millis;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

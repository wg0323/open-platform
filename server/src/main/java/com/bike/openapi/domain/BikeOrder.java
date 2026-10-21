package com.bike.openapi.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 骑行订单。
 */
@Data
@TableName("bike_order")
public class BikeOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String appKey;
    private String areaCode;
    private String orderId;
    private String sn;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal amount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

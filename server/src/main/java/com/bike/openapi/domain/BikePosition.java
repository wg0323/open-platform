package com.bike.openapi.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车辆位置上报记录。
 */
@Data
@TableName("bike_position")
public class BikePosition {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String appKey;
    private String areaCode;
    private String sn;
    private BigDecimal lng;
    private BigDecimal lat;
    private LocalDateTime positionTime;
    private Integer status;
    private Integer busiStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

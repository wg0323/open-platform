package com.bike.openapi.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 停车围栏。
 */
@Data
@TableName("fence")
public class Fence {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String areaCode;
    private String name;
    private String fence;
    private String fenceLogic;
    private Integer limits;
    private Integer fenceType;
    private String memo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

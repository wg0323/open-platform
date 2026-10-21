package com.bike.openapi.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 监管区域。
 */
@Data
@TableName("area")
public class Area {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String parentCode;
    private String fenceRange;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

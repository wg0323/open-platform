package com.bike.openapi.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接入方凭证。
 */
@Data
@TableName("app_info")
public class AppInfo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String appKey;
    private String appSecret;
    private String companyCode;
    private String companyName;
    private String ipWhitelist;
    private Integer qps;
    @TableField("is_enabled")
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

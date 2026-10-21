package com.bike.openapi.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * 修改接入方请求，字段为 null 表示不修改。
 */
@Data
@Schema(description = "修改接入方请求")
public class AppUpdateRequest {

    @Schema(description = "企业名称")
    @Size(max = 64, message = "companyName 长度不能超过 64")
    private String companyName;

    @Schema(description = "IP 白名单，逗号分隔，支持 CIDR")
    @Size(max = 512, message = "ipWhitelist 长度不能超过 512")
    private String ipWhitelist;

    @Schema(description = "每秒配额")
    @Min(value = 1, message = "qps 至少为 1")
    @Max(value = 100000, message = "qps 不能超过 100000")
    private Integer qps;

    @Schema(description = "是否启用")
    private Boolean enabled;
}

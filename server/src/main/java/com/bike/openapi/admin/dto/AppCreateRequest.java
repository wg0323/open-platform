package com.bike.openapi.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 新建接入方请求。
 */
@Data
@Schema(description = "新建接入方请求")
public class AppCreateRequest {

    @Schema(description = "企业编码", example = "11011210")
    @NotBlank(message = "companyCode 不能为空")
    @Size(max = 32, message = "companyCode 长度不能超过 32")
    private String companyCode;

    @Schema(description = "企业名称", example = "示例单车企业")
    @NotBlank(message = "companyName 不能为空")
    @Size(max = 64, message = "companyName 长度不能超过 64")
    private String companyName;

    @Schema(description = "IP 白名单，逗号分隔，支持 CIDR", example = "127.0.0.1,10.0.0.0/8")
    @Size(max = 512, message = "ipWhitelist 长度不能超过 512")
    private String ipWhitelist;

    @Schema(description = "每秒配额", example = "10")
    @Min(value = 1, message = "qps 至少为 1")
    @Max(value = 100000, message = "qps 不能超过 100000")
    private Integer qps;
}

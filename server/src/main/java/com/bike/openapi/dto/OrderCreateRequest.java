package com.bike.openapi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 上传订单请求，字段语义对齐《共享自行车接口规范 v3.0》。
 */
@Data
@Schema(description = "上传订单请求")
public class OrderCreateRequest {

    @Schema(description = "所属区域编码，行政区划六位编码", example = "110101")
    @NotBlank(message = "areaCode 不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "areaCode 必须是六位行政区划编码")
    private String areaCode;

    @Schema(description = "厂商订单号，同一 appKey 内唯一", example = "SO202610210001")
    @NotBlank(message = "orderId 不能为空")
    @Size(max = 32, message = "orderId 长度不能超过 32")
    private String orderId;

    @Schema(description = "车辆编号", example = "BK0001")
    @NotBlank(message = "sn 不能为空")
    @Size(max = 32, message = "sn 长度不能超过 32")
    private String sn;

    @Schema(description = "订单开始时间，格式 yyyy-MM-dd'T'HH:mm:ss，精确到秒、不带时区", example = "2026-10-21T09:00:00")
    @NotNull(message = "startTime 不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "订单结束时间，格式 yyyy-MM-dd'T'HH:mm:ss，精确到秒、不带时区", example = "2026-10-21T09:30:00")
    @NotNull(message = "endTime 不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "消费金额，单位元，非负，整数部分最长 8 位、最多两位小数", example = "1.50")
    @DecimalMin(value = "0", message = "amount 不能为负数")
    @Digits(integer = 8, fraction = 2, message = "amount 最多两位小数")
    private BigDecimal amount;
}

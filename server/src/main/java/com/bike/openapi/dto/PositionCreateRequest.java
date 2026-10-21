package com.bike.openapi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 上传位置请求，字段语义对齐《共享自行车接口规范 v3.0》。
 */
@Data
@Schema(description = "上传位置请求")
public class PositionCreateRequest {

    @Schema(description = "所属区域编码，行政区划六位编码", example = "110101")
    @NotBlank(message = "areaCode 不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "areaCode 必须是六位行政区划编码")
    private String areaCode;

    @Schema(description = "车辆编号", example = "BK0001")
    @NotBlank(message = "sn 不能为空")
    @Size(max = 32, message = "sn 长度不能超过 32")
    private String sn;

    @Schema(description = "经度，高德坐标", example = "116.418")
    @NotNull(message = "lng 不能为空")
    @DecimalMin(value = "-180", message = "lng 超出取值范围")
    @DecimalMax(value = "180", message = "lng 超出取值范围")
    private BigDecimal lng;

    @Schema(description = "纬度，高德坐标", example = "39.908")
    @NotNull(message = "lat 不能为空")
    @DecimalMin(value = "-90", message = "lat 超出取值范围")
    @DecimalMax(value = "90", message = "lat 超出取值范围")
    private BigDecimal lat;

    @Schema(description = "定位时间，格式 yyyy-MM-dd'T'HH:mm:ss，精确到秒、不带时区", example = "2026-10-21T09:10:00")
    @NotNull(message = "positionTime 不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime positionTime;

    @Schema(description = "车辆状态：0 开锁，1 锁止，2 锁止移动，3 正常", example = "1")
    @NotNull(message = "status 不能为空")
    @Min(value = 0, message = "status 取值 0~3")
    @Max(value = 3, message = "status 取值 0~3")
    private Integer status;

    @Schema(description = "运营状态：0 空闲，1 运营中", example = "0")
    @NotNull(message = "busiStatus 不能为空")
    @Min(value = 0, message = "busiStatus 取值 0~1")
    @Max(value = 1, message = "busiStatus 取值 0~1")
    private Integer busiStatus;
}

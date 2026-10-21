package com.bike.openapi.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.bike.openapi.domain.BikeOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图。
 */
@Data
@Schema(description = "订单信息")
public class OrderVo {

    @Schema(description = "厂商订单号")
    private String orderId;

    @Schema(description = "所属区域编码")
    private String areaCode;

    @Schema(description = "车辆编号")
    private String sn;

    @Schema(description = "订单开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "订单结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "消费金额")
    private BigDecimal amount;

    public static OrderVo from(BikeOrder order) {
        OrderVo vo = new OrderVo();
        vo.setOrderId(order.getOrderId());
        vo.setAreaCode(order.getAreaCode());
        vo.setSn(order.getSn());
        vo.setStartTime(order.getStartTime());
        vo.setEndTime(order.getEndTime());
        vo.setAmount(order.getAmount());
        return vo;
    }
}

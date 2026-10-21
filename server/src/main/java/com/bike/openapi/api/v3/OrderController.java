package com.bike.openapi.api.v3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.common.ApiResult;
import com.bike.openapi.domain.BikeOrder;
import com.bike.openapi.dto.OrderCreateRequest;
import com.bike.openapi.security.ApiAuthFilter;
import com.bike.openapi.service.BikeOrderService;
import com.bike.openapi.vo.OrderVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 订单上传接口。
 */
@Tag(name = "order", description = "订单")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/orders")
public class OrderController {

    private final BikeOrderService bikeOrderService;

    /**
     * 上传订单，必须携带 Idempotency-Key。
     *
     * @param request        订单请求
     * @param idempotencyKey 幂等键，重试时必须复用
     * @param servletRequest 原始请求，用于取 appKey
     * @return 订单视图
     */
    @Operation(summary = "上传订单", operationId = "createOrder")
    @PostMapping
    public ApiResult<OrderVo> create(@Valid @RequestBody OrderCreateRequest request,
                                     @RequestHeader(ApiAuthFilter.HEADER_IDEMPOTENCY_KEY) String idempotencyKey,
                                     HttpServletRequest servletRequest) {
        String appKey = (String) servletRequest.getAttribute(ApiAuthFilter.ATTR_APP_KEY);
        BikeOrder order = bikeOrderService.saveOrder(appKey, idempotencyKey, request);
        return ApiResult.ok(OrderVo.from(order));
    }
}

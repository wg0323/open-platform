package com.bike.openapi.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;
import com.bike.openapi.domain.BikeOrder;
import com.bike.openapi.dto.OrderCreateRequest;
import com.bike.openapi.idempotent.IdempotentService;
import com.bike.openapi.mapper.BikeOrderMapper;
import com.bike.openapi.metrics.ApiMetricsRecorder;
import com.bike.openapi.service.BikeOrderService;
import com.bike.openapi.vo.OrderVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 骑行订单服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BikeOrderServiceImpl extends ServiceImpl<BikeOrderMapper, BikeOrder> implements BikeOrderService {

    private static final String BIZ_TYPE = "order";

    private final IdempotentService idempotentService;
    private final ApiMetricsRecorder apiMetricsRecorder;

    @Override
    public BikeOrder saveOrder(String appKey, String idempotencyKey, OrderCreateRequest request) {
        if (!idempotentService.tryAcquire(BIZ_TYPE, appKey, idempotencyKey)) {
            apiMetricsRecorder.recordIdempotentHit(appKey);
            throw buildOrderExists(appKey, request.getOrderId());
        }
        try {
            BikeOrder order = buildOrder(appKey, request);
            try {
                save(order);
            } catch (DuplicateKeyException exception) {
                throw buildOrderExists(appKey, request.getOrderId());
            }
            idempotentService.markDone(BIZ_TYPE, appKey, idempotencyKey);
            return order;
        } catch (RuntimeException exception) {
            idempotentService.release(BIZ_TYPE, appKey, idempotencyKey);
            throw exception;
        }
    }

    private BikeOrder buildOrder(String appKey, OrderCreateRequest request) {
        BikeOrder order = new BikeOrder();
        order.setAppKey(appKey);
        order.setAreaCode(request.getAreaCode());
        order.setOrderId(request.getOrderId());
        order.setSn(request.getSn());
        order.setStartTime(request.getStartTime());
        order.setEndTime(request.getEndTime());
        order.setAmount(request.getAmount());
        return order;
    }

    private BizException buildOrderExists(String appKey, String orderId) {
        BikeOrder existing = getOne(Wrappers.<BikeOrder>lambdaQuery()
                .eq(BikeOrder::getAppKey, appKey)
                .eq(BikeOrder::getOrderId, orderId), false);
        Object data = existing == null ? null : OrderVo.from(existing);
        return new BizException(ErrorCode.ORDER_EXISTS, "订单已存在", data);
    }
}

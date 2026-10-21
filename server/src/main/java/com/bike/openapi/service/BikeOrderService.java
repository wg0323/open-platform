package com.bike.openapi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bike.openapi.domain.BikeOrder;
import com.bike.openapi.dto.OrderCreateRequest;

/**
 * 骑行订单服务。
 */
public interface BikeOrderService extends IService<BikeOrder> {

    /**
     * 保存订单，幂等键与数据库唯一索引双重保证不重复。
     *
     * @param appKey         接入方标识
     * @param idempotencyKey 幂等键
     * @param request        订单请求
     * @return 保存后的订单
     */
    BikeOrder saveOrder(String appKey, String idempotencyKey, OrderCreateRequest request);
}

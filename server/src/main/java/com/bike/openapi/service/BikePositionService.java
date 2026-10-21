package com.bike.openapi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bike.openapi.domain.BikePosition;
import com.bike.openapi.dto.PositionCreateRequest;

/**
 * 车辆位置服务。
 */
public interface BikePositionService extends IService<BikePosition> {

    /**
     * 保存位置上报，按（appKey, sn, positionTime）幂等覆盖。
     *
     * @param appKey  接入方标识
     * @param request 位置请求
     */
    void savePosition(String appKey, PositionCreateRequest request);
}

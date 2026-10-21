package com.bike.openapi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bike.openapi.domain.BikePosition;
import com.bike.openapi.dto.PositionCreateRequest;
import com.bike.openapi.mapper.BikePositionMapper;
import com.bike.openapi.service.BikePositionService;
import org.springframework.stereotype.Service;

/**
 * 车辆位置服务实现。
 */
@Service
public class BikePositionServiceImpl extends ServiceImpl<BikePositionMapper, BikePosition>
        implements BikePositionService {

    @Override
    public void savePosition(String appKey, PositionCreateRequest request) {
        BikePosition position = new BikePosition();
        position.setAppKey(appKey);
        position.setAreaCode(request.getAreaCode());
        position.setSn(request.getSn());
        position.setLng(request.getLng());
        position.setLat(request.getLat());
        position.setPositionTime(request.getPositionTime());
        position.setStatus(request.getStatus());
        position.setBusiStatus(request.getBusiStatus());
        baseMapper.upsert(position);
    }
}

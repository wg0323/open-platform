package com.bike.openapi.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bike.openapi.domain.Area;
import com.bike.openapi.mapper.AreaMapper;
import com.bike.openapi.service.AreaService;
import org.springframework.stereotype.Service;

/**
 * 区域服务实现。
 */
@Service
public class AreaServiceImpl extends ServiceImpl<AreaMapper, Area> implements AreaService {

    @Override
    public IPage<Area> pageAreas(long pageNo, long pageSize, Long areaId) {
        return page(new Page<>(pageNo, pageSize),
                Wrappers.<Area>lambdaQuery()
                        .eq(areaId != null, Area::getId, areaId)
                        .orderByAsc(Area::getId));
    }
}

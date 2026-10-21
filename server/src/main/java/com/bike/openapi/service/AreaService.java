package com.bike.openapi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bike.openapi.domain.Area;

/**
 * 区域服务。
 */
public interface AreaService extends IService<Area> {

    /**
     * 分页查询区域。
     *
     * @param pageNo   页码，从 1 开始
     * @param pageSize 每页条数
     * @param areaId   区域标识，为空表示不限
     * @return 分页结果
     */
    IPage<Area> pageAreas(long pageNo, long pageSize, Long areaId);
}

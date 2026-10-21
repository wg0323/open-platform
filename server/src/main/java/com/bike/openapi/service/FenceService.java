package com.bike.openapi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bike.openapi.domain.Fence;

/**
 * 围栏服务。
 */
public interface FenceService extends IService<Fence> {

    /**
     * 分页查询围栏。
     *
     * @param pageNo  页码，从 1 开始
     * @param pageSize 每页条数
     * @param fenceId 围栏标识，为空表示不限
     * @return 分页结果
     */
    IPage<Fence> pageFences(long pageNo, long pageSize, Long fenceId);
}

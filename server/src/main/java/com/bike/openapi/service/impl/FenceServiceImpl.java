package com.bike.openapi.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bike.openapi.domain.Fence;
import com.bike.openapi.mapper.FenceMapper;
import com.bike.openapi.service.FenceService;
import org.springframework.stereotype.Service;

/**
 * 围栏服务实现。
 */
@Service
public class FenceServiceImpl extends ServiceImpl<FenceMapper, Fence> implements FenceService {

    @Override
    public IPage<Fence> pageFences(long pageNo, long pageSize, Long fenceId) {
        return page(new Page<>(pageNo, pageSize),
                Wrappers.<Fence>lambdaQuery()
                        .eq(fenceId != null, Fence::getId, fenceId)
                        .orderByAsc(Fence::getId));
    }
}

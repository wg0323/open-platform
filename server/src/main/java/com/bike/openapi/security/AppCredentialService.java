package com.bike.openapi.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.bike.openapi.domain.AppInfo;
import com.bike.openapi.service.AppInfoService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 凭证缓存：空值一并缓存，避免伪造 appKey 打穿数据库。
 */
@Service
public class AppCredentialService {

    private static final long CACHE_SECONDS = 60;
    private static final long CACHE_SIZE = 512;

    private final AppInfoService appInfoService;
    private final Cache<String, Optional<AppInfo>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_SECONDS, TimeUnit.SECONDS)
            .maximumSize(CACHE_SIZE)
            .build();

    public AppCredentialService(AppInfoService appInfoService) {
        this.appInfoService = appInfoService;
    }

    /**
     * 查询凭证。
     *
     * @param appKey 接入方标识
     * @return 凭证
     */
    public Optional<AppInfo> find(String appKey) {
        try {
            return cache.get(appKey, () -> Optional.ofNullable(appInfoService.getByAppKey(appKey)));
        } catch (ExecutionException exception) {
            throw new IllegalStateException("加载接入方凭证失败", exception);
        }
    }

    /**
     * 管理台修改配置后清空缓存，最迟 60 秒内在其他实例生效。
     */
    public void invalidateAll() {
        cache.invalidateAll();
    }
}

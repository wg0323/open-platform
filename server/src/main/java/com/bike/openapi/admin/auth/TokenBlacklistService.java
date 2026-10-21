package com.bike.openapi.admin.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 已登出 token 的黑名单，TTL 与 token 剩余有效期一致。
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "admin:jwt:blacklist:";
    private static final String PRESENT = "1";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String jti, long ttlSeconds) {
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(KEY_PREFIX + jti, PRESENT, ttlSeconds, TimeUnit.SECONDS);
        }
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}

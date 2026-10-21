package com.bike.openapi.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Nonce 防重放存储，必须在验签通过之后写入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NonceService {

    private static final String KEY_PREFIX = "nonce:";
    private static final String COLON = ":";
    private static final String VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    /**
     * 写入 nonce。
     *
     * @param appKey     接入方标识
     * @param nonce      一次性随机串
     * @param ttlSeconds 存活秒数，必须大于时间窗口
     * @return true 表示首次出现
     */
    public boolean markIfAbsent(String appKey, String nonce, int ttlSeconds) {
        try {
            Boolean created = redisTemplate.opsForValue()
                    .setIfAbsent(KEY_PREFIX + appKey + COLON + nonce, VALUE, Duration.ofSeconds(ttlSeconds));
            return Boolean.TRUE.equals(created);
        } catch (DataAccessException exception) {
            log.error("Nonce 写入失败", exception);
            throw new BizException(ErrorCode.REDIS_UNAVAILABLE);
        }
    }
}

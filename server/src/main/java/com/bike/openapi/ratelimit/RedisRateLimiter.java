package com.bike.openapi.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Redis 令牌桶限流，多实例共享同一配额。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private static final int KEY_TTL_SECONDS = 5;
    private static final long ALLOWED = 1L;
    private static final int MILLIS_PER_SECOND = 1000;

    private static final String LUA_SCRIPT =
            "local key = KEYS[1] "
            + "local capacity = tonumber(ARGV[1]) "
            + "local qps = tonumber(ARGV[2]) "
            + "local now = tonumber(ARGV[3]) "
            + "local ttl = tonumber(ARGV[4]) "
            + "local bucket = redis.call('HMGET', key, 'tokens', 'ts') "
            + "local tokens = tonumber(bucket[1]) "
            + "local ts = tonumber(bucket[2]) "
            + "if tokens == nil then tokens = capacity ts = now end "
            + "local delta = now - ts "
            + "if delta < 0 then delta = 0 end "
            + "tokens = math.min(capacity, tokens + delta * qps / " + MILLIS_PER_SECOND + ") "
            + "local allowed = 0 "
            + "if tokens >= 1 then tokens = tokens - 1 allowed = 1 end "
            + "redis.call('HMSET', key, 'tokens', tokens, 'ts', now) "
            + "redis.call('EXPIRE', key, ttl) "
            + "return allowed";

    private static final DefaultRedisScript<Long> SCRIPT =
            new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试获取一个令牌。
     *
     * @param key 限流键
     * @param qps 每秒配额，非正数表示不限制
     * @return 是否放行
     */
    public boolean tryAcquire(String key, int qps) {
        if (qps <= 0) {
            return true;
        }
        try {
            Long allowed = redisTemplate.execute(SCRIPT, Collections.singletonList(key),
                    String.valueOf(qps), String.valueOf(qps),
                    String.valueOf(System.currentTimeMillis()), String.valueOf(KEY_TTL_SECONDS));
            return allowed != null && allowed == ALLOWED;
        } catch (DataAccessException exception) {
            log.error("限流脚本执行失败, key={}", key, exception);
            throw new BizException(ErrorCode.REDIS_UNAVAILABLE);
        }
    }
}

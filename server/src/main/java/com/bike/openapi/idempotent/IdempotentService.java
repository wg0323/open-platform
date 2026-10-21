package com.bike.openapi.idempotent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 幂等键存储：Redis 只是快路径，数据库唯一索引才是最终防线。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentService {

    private static final String KEY_PREFIX = "idem:";
    private static final String STATE_PROCESSING = "PROCESSING";
    private static final String STATE_DONE = "DONE";
    private static final Duration TTL = Duration.ofHours(24);
    private static final String COLON = ":";

    private final StringRedisTemplate redisTemplate;

    /**
     * 抢占幂等键。
     *
     * @param bizType        业务类型
     * @param appKey         接入方标识
     * @param idempotencyKey 幂等键
     * @return true 表示首次抢占成功
     */
    public boolean tryAcquire(String bizType, String appKey, String idempotencyKey) {
        Boolean acquired = execute(() -> redisTemplate.opsForValue()
                .setIfAbsent(buildKey(bizType, appKey, idempotencyKey), STATE_PROCESSING, TTL));
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 标记处理完成。
     *
     * @param bizType        业务类型
     * @param appKey         接入方标识
     * @param idempotencyKey 幂等键
     */
    public void markDone(String bizType, String appKey, String idempotencyKey) {
        execute(() -> {
            redisTemplate.opsForValue().set(buildKey(bizType, appKey, idempotencyKey), STATE_DONE, TTL);
            return Boolean.TRUE;
        });
    }

    /**
     * 业务失败时释放幂等键，允许调用方重试。
     *
     * @param bizType        业务类型
     * @param appKey         接入方标识
     * @param idempotencyKey 幂等键
     */
    public void release(String bizType, String appKey, String idempotencyKey) {
        execute(() -> redisTemplate.delete(buildKey(bizType, appKey, idempotencyKey)));
    }

    private String buildKey(String bizType, String appKey, String idempotencyKey) {
        return KEY_PREFIX + bizType + COLON + appKey + COLON + idempotencyKey;
    }

    private <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (DataAccessException exception) {
            log.error("Redis 操作失败", exception);
            throw new BizException(ErrorCode.REDIS_UNAVAILABLE);
        }
    }
}

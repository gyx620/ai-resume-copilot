package com.gyx.seckill.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RedisSeckillStockService {

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String BUYERS_KEY_PREFIX = "seckill:buyers:";

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>();

    static {
        RESERVE_SCRIPT.setLocation(new ClassPathResource("lua/seckill-reserve.lua"));
        RESERVE_SCRIPT.setResultType(Long.class);
        RELEASE_SCRIPT.setLocation(new ClassPathResource("lua/seckill-release.lua"));
        RELEASE_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    public RedisSeckillStockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long reserve(Long activityId, Long userId) {
        Long result = redisTemplate.execute(
                RESERVE_SCRIPT,
                List.of(stockKey(activityId), buyersKey(activityId)),
                userId.toString());
        if (result == null) {
            throw new IllegalStateException("Redis 秒杀脚本没有返回结果");
        }
        return result;
    }

    public void release(Long activityId, Long userId) {
        redisTemplate.execute(
                RELEASE_SCRIPT,
                List.of(stockKey(activityId), buyersKey(activityId)),
                userId.toString());
    }

    public void initializeStock(Long activityId, Integer stock) {
        // 只在 Redis 中没有库存键时初始化，避免应用重启时覆盖正在使用的库存。
        redisTemplate.opsForValue().setIfAbsent(stockKey(activityId), stock.toString());
    }

    public boolean isReserved(Long activityId, Long userId) {
        Boolean reserved = redisTemplate.opsForSet().isMember(buyersKey(activityId), userId.toString());
        return Boolean.TRUE.equals(reserved);
    }

    public static String stockKey(Long activityId) {
        return STOCK_KEY_PREFIX + activityId;
    }

    public static String buyersKey(Long activityId) {
        return BUYERS_KEY_PREFIX + activityId;
    }
}

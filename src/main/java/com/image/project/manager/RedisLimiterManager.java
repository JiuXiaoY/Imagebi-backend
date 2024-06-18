package com.image.project.manager;

import com.image.project.common.ErrorCode;
import com.image.project.exception.ThrowUtils;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/7
 */
@Service
public class RedisLimiterManager {
    @Resource
    private RedissonClient redissonClient;

    public void doRateLimit(String key) {
        // 创建一个名为 key 的限流器，每秒最多访问两次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 出现这个问题的原因可能是限流器的速率已经设置过了，而 trySetRate 方法在尝试重新设置速率时失败了，
        // 从而抛出了 REDIS_INTERNAL_ERROR。这是因为 trySetRate 只能在限流器没有被配置速率时成功，
        // 如果已经配置了速率，再次调用就会失败。
        // boolean b = rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);
        // 检查限流器是否已经设置过速率
        if (!rateLimiter.isExists()) {
            boolean b = rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);
            ThrowUtils.throwIf(!b, ErrorCode.REDIS_INTERNAL_ERROR);
        }

        boolean canPass = rateLimiter.tryAcquire(1);
        ThrowUtils.throwIf(!canPass, ErrorCode.TOO_MANY_REQUEST);
    }
}

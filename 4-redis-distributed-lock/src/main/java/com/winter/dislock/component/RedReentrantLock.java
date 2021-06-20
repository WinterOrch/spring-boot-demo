package com.winter.dislock.component;

import com.winter.dislock.common.consts.RedConst;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.TimeoutUtils;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class RedReentrantLock {
    private final StringRedisTemplate strRedisTemplate;

    private final RedisScript<Boolean> tryLockScript;
    private final RedisScript<Boolean> releaseLockScript;

    private final String LOC_PREFIX = RedConst.REDIS_DIS_REENTRANT_PREFIX;
    private final String RED_SEPARATOR = RedConst.SEPARATOR;

    public RedReentrantLock(StringRedisTemplate strRedisTemplate,
                   RedisScript<Boolean> reentrantTryLockRedisScript, RedisScript<Boolean> reentrantReleaseLockRedisScript) {
        this.strRedisTemplate = strRedisTemplate;
        this.tryLockScript = reentrantTryLockRedisScript;
        this.releaseLockScript = reentrantReleaseLockRedisScript;
    }

    public Boolean tryLock(String partKey, String distributedId, long timeout, TimeUnit unit) {
        return this.strRedisTemplate.execute(this.tryLockScript,
                Collections.singletonList(LOC_PREFIX + RED_SEPARATOR + partKey),
                distributedId, TimeoutUtils.toSeconds(timeout, unit));
    }

    public Boolean releaseLock(String partKey, String distributedId, long timeout, TimeUnit unit) {
        return this.strRedisTemplate.execute(this.releaseLockScript,
                Collections.singletonList(LOC_PREFIX + RED_SEPARATOR + partKey),
                distributedId, TimeoutUtils.toSeconds(timeout, unit));
    }
}

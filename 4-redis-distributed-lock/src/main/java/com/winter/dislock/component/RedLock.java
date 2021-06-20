package com.winter.dislock.component;

import com.winter.dislock.common.consts.RedConst;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class RedLock {

    private final StringRedisTemplate strRedisTemplate;

    private final RedisScript<Boolean> tryLockScript;
    private final RedisScript<Boolean> releaseLockScript;

    public RedLock(StringRedisTemplate strRedisTemplate,
                   RedisScript<Boolean> tryLockRedisScript, RedisScript<Boolean> releaseLockRedisScript) {
        this.strRedisTemplate = strRedisTemplate;
        this.tryLockScript = tryLockRedisScript;
        this.releaseLockScript = releaseLockRedisScript;
    }

    public Boolean tryLock(String partKey, String value) {
        return this.strRedisTemplate.execute(this.tryLockScript,
                Arrays.asList(RedConst.REDIS_DIS_LOCK_PREFIX + RedConst.SEPARATOR + partKey, value));
    }

    public Boolean releaseLock(String partKey, String value) {
        return this.strRedisTemplate.execute(this.releaseLockScript,
                Arrays.asList(RedConst.REDIS_DIS_LOCK_PREFIX + RedConst.SEPARATOR + partKey, value));
    }
}

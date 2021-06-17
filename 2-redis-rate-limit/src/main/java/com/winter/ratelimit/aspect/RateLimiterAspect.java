package com.winter.ratelimit.aspect;

import com.winter.ratelimit.annotation.RateLimit;

import cn.hutool.core.util.StrUtil;
import com.winter.ratelimit.common.consts.RedConst;
import com.winter.ratelimit.common.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RateLimiterAspect {
    private final StringRedisTemplate strRedisTemplate;
    private final RedisScript<Long> limitRedisScript;

    public RateLimiterAspect(StringRedisTemplate strRedisTemplate, RedisScript<Long> limitRedisScript) {
        this.strRedisTemplate = strRedisTemplate;
        this.limitRedisScript = limitRedisScript;
    }

    @Pointcut("@annotation(com.winter.ratelimit.annotation.RateLimit)")
    public void rateLimit() {}

    @Around("rateLimit()")
    public Object pointcut(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        // Fetch annotation RateLimit
        RateLimit rateLimit = AnnotationUtils.findAnnotation(method, RateLimit.class);
        if (rateLimit != null) {
            String key = rateLimit.key();
            if (StrUtil.isBlank(key)) {
                key = method.getDeclaringClass().getName() + StrUtil.DOT + method.getName();
            }
            // SUFFIX + IpAddr
            key = key + RedConst.SEPARATOR + IpUtil.getIpAddr();

            long max = rateLimit.max();
            long timeout = rateLimit.timeout();
            TimeUnit timeUnit = rateLimit.timeUnit();
            if (shouldBeLimited(key, max, timeout, timeUnit)) {
                throw new RuntimeException("Slow down, bro, slow down.");
            }
        }
        return point.proceed();
    }

    private boolean shouldBeLimited(String key, long max, long timeout, TimeUnit timeUnit) {
        // Final Key:
        //  limit:{key}:{ip}
        key = RedConst.REDIS_LIMIT_KEY_PREFIX + RedConst.SEPARATOR + key;

        // Generic to Millis
        long ttl = timeUnit.toMillis(timeout);
        long now = Instant.now().toEpochMilli();
        long expired = now - ttl;

        Long executeTimes = this.strRedisTemplate.execute(limitRedisScript, Collections.singletonList(key),
                now + "", ttl + "", expired + "", max + "");
        if (executeTimes != null) {
            if (executeTimes == 0) {
                log.error("[{}] has reached visit limit in {} millis, with current limit {}", key, ttl, max);
                return true;
            } else {
                log.info("[{}] has reached visit {} times in {} millis", key, executeTimes, ttl);
                return false;
            }
        }
        return false;
    }
}

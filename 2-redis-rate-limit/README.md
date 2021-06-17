> 参考了 [Java并发：分布式应用限流 Redis + Lua 实践](https://segmentfault.com/a/1190000016042927) 

主要演示了 Spring Boot 项目如何通过 AOP 结合 Redis + Lua 脚本实现分布式限流，旨在保护 API 不受恶意频繁访问影响。用 `guava` 也可以完成，但相对性能差一些。

## Lua 脚本

参考： [聊聊高并发系统之限流特技](http://jinnianshilongnian.iteye.com/blog/2305117)
http://jinnianshilongnian.iteye.com/blog/2305117

```lua
local key = "rate.limit:" .. KEYS[1] --限流KEY
local limit = tonumber(ARGV[1])        --限流大小
local current = tonumber(redis.call('get', key) or "0")
if current + 1 > limit then --如果超出限流大小
  return 0
else  --请求数+1，并设置2秒过期
  redis.call("INCRBY", key,"1")
   redis.call("expire", key,"2")
   return current + 1
end
```

1. 通过KEYS[1] 获取传入的key参数
2. 通过ARGV[1]获取传入的limit参数
3. redis.call方法，从缓存中get和key相关的值，如果为nil那么就返回0
4. 判断缓存中记录的数值是否会大于限制大小，如果超出表示该被限流，返回0
5. 如果未超过，那么该key的缓存值+1，并设置过期时间为1秒钟以后，并返回缓存值+1

---

## 拦截器

通过拦截器 拦截`@RateLimit`注解的方法，使用`Redsi execute` 方法执行限流脚本，判断是否超过限流次数

```java
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

```


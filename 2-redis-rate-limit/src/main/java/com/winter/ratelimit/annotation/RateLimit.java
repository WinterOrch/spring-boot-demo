package com.winter.ratelimit.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    long DEFAULT_REQUEST = 10;

    /**
     * value for max requests
     * */
    @AliasFor("max") long value() default DEFAULT_REQUEST;

    /**
     * max for largest requests
     * */
    @AliasFor("value") long max() default DEFAULT_REQUEST;

    /**
     * key for rate limit
     * */
    String key() default "";

    long timeout() default 1L;

    TimeUnit timeUnit() default TimeUnit.MINUTES;
}

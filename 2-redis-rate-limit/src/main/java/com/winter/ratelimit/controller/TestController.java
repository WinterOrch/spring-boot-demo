package com.winter.ratelimit.controller;

import cn.hutool.core.lang.Dict;
import com.winter.ratelimit.annotation.RateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    protected static Logger logger = LoggerFactory.getLogger(TestController.class);

    @RateLimit(value = 5, key = "test_key_01")
    @GetMapping("/test/ratelimit")
    public Dict testWithLimit() {
        logger.info("OK, I'm executed!");
        return Dict.create().set("msg", "Hello!").set("description", "You won't be able to refresh forever.");
    }
}

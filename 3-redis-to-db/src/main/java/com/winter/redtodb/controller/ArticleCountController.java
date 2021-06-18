package com.winter.redtodb.controller;

import com.winter.redtodb.controller.request.PostArticleViewsRequest;
import com.winter.redtodb.service.ArticleCountService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ArticleCountController {
    private final ArticleCountService articleCountService;

    public ArticleCountController(ArticleCountService articleCountService) {
        this.articleCountService = articleCountService;
    }

    /**
     * Record user reading article in redis
     * created in 15:01 2021/6/18
     */
    @RequestMapping(value = "/post/article/views", method = RequestMethod.POST)
    public Object postArticleViews(@RequestBody @Validated PostArticleViewsRequest postArticleViewsRequest) {
        return this.articleCountService.postArticleViews(postArticleViewsRequest);
    }

    /**
     * Post reading records from redis to db in batches
     * created in 15:02 2021/6/18
     */
    @RequestMapping(value = "/post/batch", method = RequestMethod.POST)
    public Object postToDbViews() {
        return this.articleCountService.postBatchedRedisToDb();
    }
}

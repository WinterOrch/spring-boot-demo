package com.winter.redtodb.service;

import com.winter.redtodb.controller.request.PostArticleViewsRequest;

public interface ArticleCountService {
    Object postArticleViews(PostArticleViewsRequest postArticleViewsRequest);

    Object postBatchedRedisToDb();
}

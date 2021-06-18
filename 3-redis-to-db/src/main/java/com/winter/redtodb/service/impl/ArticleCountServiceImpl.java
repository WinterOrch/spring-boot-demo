package com.winter.redtodb.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.winter.redtodb.common.consts.RedConst;
import com.winter.redtodb.common.wrapper.WrapMapper;
import com.winter.redtodb.controller.request.PostArticleViewsRequest;
import com.winter.redtodb.dto.ArticleCountDto;
import com.winter.redtodb.mybatis.mapper.ArticleCountMapper;
import com.winter.redtodb.service.ArticleCountService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ArticleCountServiceImpl implements ArticleCountService {
    private final RedisTemplate<String, String> strRedisTemplate;
    private final ArticleCountMapper articleCountMapper;

    public ArticleCountServiceImpl(RedisTemplate<String, String> strRedisTemplate, ArticleCountMapper articleCountMapper) {
        this.strRedisTemplate = strRedisTemplate;
        this.articleCountMapper = articleCountMapper;
    }

    @Override
    public Object postArticleViews(PostArticleViewsRequest postArticleViewsRequest) {
        ArticleCountDto articleCount = new ArticleCountDto();
        articleCount.setBuNo(IdUtil.simpleUUID());
        articleCount.setCustomerId(postArticleViewsRequest.getCustomerId());
        articleCount.setArticleNo(postArticleViewsRequest.getArticleNo());
        articleCount.setReadTime(new Date());
        String strArticleCountDto = JSONUtil.toJsonStr(articleCount);
        strRedisTemplate.opsForList().rightPush(RedConst.ARTICLE_COUNT_REDIS_KEY, strArticleCountDto);
        return WrapMapper.ok();
    }

    @Override
    public Object postBatchedRedisToDb() {
        Date current = new Date();
        while (true) {
            List<String> strArticleCountList = strRedisTemplate.opsForList().range(RedConst.ARTICLE_COUNT_REDIS_KEY,
                    0L, RedConst.ARTICLE_READ_NUM);
            if (CollectionUtils.isEmpty(strArticleCountList)) {
                return WrapMapper.ok();
            }

            List<ArticleCountDto> articleCountDtoList = new ArrayList<>();
            for (String strArticleCount : strArticleCountList) {
                ArticleCountDto output = JSON.parseObject(strArticleCount, ArticleCountDto.class);

                // Only article read before task started shall be counted
                if (output.getReadTime().before(current)) {
                    articleCountDtoList.add(output);
                }
            }

            if (CollectionUtils.isEmpty(articleCountDtoList)) {
                return WrapMapper.ok();
            }

            this.articleCountMapper.batchedInsert(articleCountDtoList);
            strRedisTemplate.opsForList().trim(RedConst.ARTICLE_COUNT_REDIS_KEY, articleCountDtoList.size(), -1L);
        }
    }
}

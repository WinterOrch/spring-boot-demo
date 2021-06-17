package com.winter.visitor.service;

import com.winter.visitor.common.consts.RedisKey;
import com.winter.visitor.vo.VisitorVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class VisitingService {
    /*private static final int BLOOM_FILTER_EXPECTED_INSERTION = 1500;
    private static final double BLOOM_FILTER_FPP = 0.001;*/

    private final RedisTemplate<String, Long> redisTemplate;
    private final RedisTemplate<String, String> strRedisTemplate;

    public VisitingService(RedisTemplate<String, Long> longRedisTemplate,
                           RedisTemplate<String, String> strRedisTemplate) {
        this.redisTemplate = longRedisTemplate;
        this.strRedisTemplate = strRedisTemplate;
    }

    /**
     * 获取pv
     * pv存储结果为hash，一个应用一个key; field 为uri； value为pv
     *
     * @return null表示首次有人访问；这个时候需要+1
     */
    public Long fetchPV(String key, String uri) {
        Object val = this.redisTemplate.opsForHash().get(key, uri);
        return val == null ? null : (Long) val;
    }

    public void incrPV(String key, String uri) {
        this.redisTemplate.opsForHash().increment(key, uri, 1);
    }

    /**
     * 获取 uri对应的uv，以及当前访问ip的历史访问排名
     * 使用zset来存储，key为uri唯一标识；value为ip；score为访问的排名
     *
     * @return 返回VisitorVO, 但是其中只有 unique_vis 和 rank 两个字段有效
     */
    public VisitorVO fetchUV(String app, String uri, String ip) {
        String key = RedisKey.URI_RANK_PREFIX + app + "_" + uri;
        Set<ZSetOperations.TypedTuple<String>> set = this.strRedisTemplate.opsForZSet().rangeWithScores(key, -1, -1);

        Long unique_visitors = null;
        if (null == set || CollectionUtils.isEmpty(set)) {
            unique_visitors = 0L;
        } else {
            for (ZSetOperations.TypedTuple<String> op : set) {
                Double score = op.getScore();
                unique_visitors = (score == null) ? null : score.longValue();
                break;
            }
        }

        VisitorVO res = new VisitorVO();
        if (unique_visitors == null || unique_visitors == 0L) {
            res.setUnique_vis(0L);
            res.setRank(0L);
            return res;
        } else {
            res.setUnique_vis(unique_visitors);
        }

        Double score = this.strRedisTemplate.opsForZSet().score(key, ip);
        res.setRank((score == null) ? 0L : score.longValue());
        return res;
    }

    public void setUV(String key, String ip, Long rank) {
        this.strRedisTemplate.opsForZSet().add(key, ip, rank);
    }

    /**
     * 判断ip今天是否访问过
     * 不采用 com.google.common.hash.BloomFilter 来判断了
     * HyperLogLog 真香
     *
     * @return true 表示今天访问过/ false 表示今天没有访问过
     */
    public boolean visitToday(String key, String ip) {
        Boolean doesLogTodayExist = this.strRedisTemplate.hasKey(key);
        if (doesLogTodayExist != null && doesLogTodayExist) {
            return (this.strRedisTemplate.opsForHyperLogLog().add(key, ip)) > 0L;
        } else {
            this.strRedisTemplate.opsForHyperLogLog().add(key, ip);
            this.strRedisTemplate.expire(key, 1L, TimeUnit.DAYS);
            return false;
        }
        /*Serializable result = this.redisCacheTemplate.opsForValue().get(key);
        if (result == null) {
            // 今日还没有访问，创建布隆过滤器，存入缓存
            BloomFilter<String> filterToday = BloomFilter.create(
                    Funnels.stringFunnel(Charsets.UTF_8),
                    BLOOM_FILTER_EXPECTED_INSERTION,
                    BLOOM_FILTER_FPP);
            filterToday.put(ip);
            this.redisCacheTemplate.opsForValue().set(key, filterToday, 1, TimeUnit.DAYS);
            return false;
        } else {
            // 今日有访问，取布隆过滤器
            BloomFilter<String> filterToday = (BloomFilter) result;
            return filterToday.mightContain(ip);
        }*/
    }

    /*public void markVisitToday(String key, String ip) {
        Serializable result = this.redisCacheTemplate.opsForValue().get(key);
        BloomFilter<String> filterToday;
        if (result == null) {
            filterToday = BloomFilter.create(
                    Funnels.stringFunnel(Charsets.UTF_8),
                    BLOOM_FILTER_EXPECTED_INSERTION,
                    BLOOM_FILTER_FPP);
        } else {
            filterToday = (BloomFilter) result;
        }
        filterToday.put(ip);
        this.redisCacheTemplate.opsForValue().set(key, filterToday, 1, TimeUnit.DAYS);
    }*/

    /**
     * 每次访问，热度加一
     */
    public Long incrTemp(String key, String uri) {
        return this.redisTemplate.opsForHash().increment(key, uri, 1);
    }
}

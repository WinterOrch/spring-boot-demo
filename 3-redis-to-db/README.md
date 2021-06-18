> Redis 负责日常读写，定时批量持久化到 DB 中。参考自 https://zhanghan.blog.csdn.net/article/details/107878941

浏览记录写入缓存，模拟缓存定时存入数据库的形式。如下总结三种缓存工作模式，这里用的是第三种异步，因为数据不需要频繁同步，且能容许少量错误。

> #### 一、旁路缓存模式 (Cache Aside Pattern)
>
> 适用**读请求较多**的场景
>
> **写：**
>
> - 先更新 DB 中数据
> - 直接删除 cache
>
> > 之所以先更新 DB ，是因为 cache 的删除操作相对快很多，数据不一致的可能性大大降低。相反，如果先删除 cache，此时如果有并行请求直接从 DB 中读取数据，这一操作很可能在 DB 中数据被更新前完成。
>
> **读：**
>
> - 从 cache 中读取数据，读到直接返回
> - 读不到就从 DB 中读取并返回
> - 数据放到 cache 中
>
> 缺点一、 首次请求数据一定不在 cache ，但是这一问题可以通过热点数据的提前缓存解决。
>
> 缺点二、 写操作如果频繁，则 cache 数据被频繁删除，缓存命中率降低，缓存很大程度上被架空。在强一致场景下需要锁/分布锁保证更新 cache 时不存在线程问题；弱一致场景下可以 cache 和 DB 一起更新，cache 设置较短的过期事件以提高缓存命中率。
>
> #### 二、读写穿透模式 (Read/Write Through Pattern)
>
> cache 负责将数据读取和写入 DB，作为服务端和 DB 间的中间件。然而相当难实现，因为 Redis 不提供 DB 读写功能。
>
> **写：**
>
> - 查 cache，不存在则直接更新 DB
> - cache 存在，则先更新 cache，cache 服务自己更新 DB（cache 和 DB 同步更新）
>
> **读：**
>
> - 从 cache 读数据，读到直接返回
> - 没读到就从 DB 加载到 cache，然后返回响应
>
> 由于 Redis 不提供 DB 读写，这一模式实际上只是在旁路模式上进行了封装。同样具有首次请求数据不在 cache 问题。
>
> #### 三、异步缓存写入 (Write Behind Pattern)
>
> 和 读写穿透模式 相似，但只更新缓存，不直接更新 DB，用异步批量的方式来更新 DB。消息队列中消息异步写入磁盘、MySQL 的 InnoDB Buffer Pool 机制都用到这种策略。
>
> DB 的写性能非常高，适合数据频繁变化，数据一致性要求又不高的场景，如浏览量、点赞量。
>
> 缺点很明显，数据一致性很难维护，cache 可能在数据异步更新前宕机。

---

### Redis 读写

用的是 RedisTemplate<String, String> 模板，value 通过 JSON 序列化，借用 hutool 和 alibaba fastjson 进行正反序列化。

---

#### 批量入库

用 MyBatis 的 @InsertProvider ，动态SQL

```java
private static final MessageFormat mf = new MessageFormat(
        "(#'{'list[{0}].buNo},#'{'list[{0}].customerId},#'{'list[{0}].articleNo},#'{'list[{0}].readTime})");
public String batchedInsertSql(Map<String, Object> map) {
    int listSize = ((List)map.get("list")).size();
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("INSERT INTO zh_article_count ");
    sqlBuilder.append("(bu_no, customer_id, article_no, read_time) ");
    sqlBuilder.append("VALUES ");
    for (int i = 0; i < listSize; ++i) {
        sqlBuilder.append(mf.format(new Object[]{i}));
        if (i < listSize - 1) {
            sqlBuilder.append(",");
        }
    }
    return sqlBuilder.toString().trim();
}
```


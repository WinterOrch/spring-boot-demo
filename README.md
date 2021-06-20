# Spring Boot Demo
Demo Cases for Springboot Learning

## 

## Redis

| [1-redis-visitor-count](https://github.com/WinterOrch/spring-boot-demo/tree/master/1-redis-visitor-count) | 访客数量   |
| ------------------------------------------------------------ | ---------- |
| [2-redis-rate-limit](https://github.com/WinterOrch/spring-boot-demo/tree/master/2-redis-rate-limit) | API限流    |
| [3-redis-to-db](https://github.com/WinterOrch/spring-boot-demo/tree/master/3-redis-to-db) | 批量持久化 |
| [4-redis-distributed-lock](https://github.com/WinterOrch/spring-boot-demo/tree/master/4-redis-distributed-lock) | 分布式锁   |

---

### 1. Site Count

记录网站中各 URI 的访问情况

- 通过 Redis ~~BloomFilter~~ HyperLogLog 判断 IP 当天是否访问过。



### 2. Rate Limit

Redis 控制 API 访问次数

- 通过 Lua 脚本维护一个记录访问次数的 ZSet ，以访问时间为 score 并定期清除过期元素，通过 ZSet 内记录总数判断 API 窗口内访问次数。 



### 3. Cache to DB

缓存批量写入数据库

- MyBatis 动态 SQL



### 4. Distributed Lock

Redis 分布式锁
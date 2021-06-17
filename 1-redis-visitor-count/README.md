> 参考了 [Redis SiteCount 项目](https://github.com/liuyueyi/spring-boot-demo/tree/master/spring-case/124-redis-sitecount) 

目标是为网站中每一 URI 统计三个量

1. pv

   `page visit` 每个页面的访问次数，一个合法 ip 一天之内的访问只统计为一次

   > 也就是说，如果一个人在十二点前后分别进行的访问会被计算为两次

2. uv

   `unique visit` 也就是访问过该 URI 的 ip 数，一个合法 ip 仅统计一次

3. temperature

   单个 URI 的热度，换句话说就是点击量，访问一次就加一

原项目提供了一个很好的 Redis 练习场景，但也存在一些问题

1. ip 过滤有问题

   因为严格限定每天只算一次访问，因此每天都需要一个过滤器来判断当前 ip 有没有访问过。原项目用的是四个 `bitmap` 分别判断 ip 中的四段8位地址是否访问过，这个明显有误，如添加了 `192.168.3.2` 和 `192.168.2.1` 两个地址后，`192.168.3.1` 也会被认为访问过。除非 `bitmap` 长度足以涵盖所有 ip 地址，否则这种方法是不可行的，必须另辟蹊径。

   首先想到了 Redis 原生的 `Hyperloglog` ，当然 `Hyperloglog` 是可以用来统计日活的，但是原项目的想法是——先判断当前 ip 当天有没有访问，再进行相应统计。这和 `Hyperloglog` 的目标有些不符——如果在 `Hyperloglog` 基础上进行重构，应当这么设计：

   ```java
   long dailyCheck = this.strRedisTemplate.opsForHyperLogLog().size(key);
   this.strRedisTemplate.opsForHyperLogLog().add(key, ip);
   return dailyCheck == this.strRedisTemplate.opsForHyperLogLog().size(key);
   ```

   因为 `Hyperloglog` 本身没有提供 `contain` 方法，只能退而求其次，用三次通信来实现（当然其中也完成了标记日活的操作，其实效率是挺高的）。

   既然如此，我们就来试试新东西吧，用 google guava 包的 `BloomFilter`，通过存一个 `Serializable` ，把布隆过滤器存到 Redis 里。需要的时候再取出进行过滤，判断当前 ip 当日是否访问过。缺点——当然了，`BloomFilter` 精确度不高，且精确是以开销为代价的。

   当然，如果能通过插件在 Redis 里加入 BloomFilter 就更好了，不过就不能用 Template 操作了。

   > 果然还是 `Hyperloglog` 好，下次不整这些有的没得了，`BloomFilter` 还被打上了 `UnstableApiUsage` 标签，搞得到处是 Warning，有机会重构一下吧。实际写到后面也发现判断用户是否访问过和标记访问其实是没必要分离的，原项目架构还是有很多问题的。

2. 项目结构有问题

   原项目包结构设置有些杂乱，加上还用到 `ImmutablePair` 这种离奇对象传参，可读性比较差，大部分代码都进行了重构。
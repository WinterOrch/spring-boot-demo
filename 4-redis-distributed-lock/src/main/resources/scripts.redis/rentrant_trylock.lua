--
-- Created by IntelliJ IDEA.
-- User: user
-- Date: 2021/6/20
-- Time: 16:14
-- To change this template use File | Settings | File Templates.
--

--[[
可重入加锁：
思路：
   1.用3个局部变量接受参数
   2.由于redis内置lua解析器，执行加锁命令
   3.如果锁不存在，直接加锁(hash)，设置锁过期事件
   4.如果锁存在，看是不是属于自己(hash中存不存在自己)
   5.   如果存在，加一，表示重入
]]
local key = KEYS[1]
local distributedId = ARGV[1]
local expiration = ARGV[2]

if (redis.call('EXISTS', key) == 0) then
    redis.call('HSET', key, distributedId, '1');
    redis.call('expire', key, expiration);
    return true;
end;

if (redis.call('HEXISTS', key, distributedId) == 1) then
    redis.call('HINCRBY', key, distributedId, '1');
    redis.call('expire', key, expiration);
    return true;
end;

return false;
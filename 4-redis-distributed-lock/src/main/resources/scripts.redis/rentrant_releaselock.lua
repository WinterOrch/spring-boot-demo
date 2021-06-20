--
-- Created by IntelliJ IDEA.
-- User: user
-- Date: 2021/6/20
-- Time: 16:31
-- To change this template use File | Settings | File Templates.
--

--[[
可重入释放：
思路：
   1.用2个局部变量接受参数
   2.由于redis内置lua解析器，执行释放锁命令
   3.如果锁不存在，返回 false
   4.如果锁存在，重入次数减一
   5.   如果重入次数为零，删除
]]
local key = KEYS[1]
local distributedId = ARGV[1]

if (redis.call('HEXISTS', key, distributedId) == 0) then
    return false;
end;

if (redis.call('HINCRBY', key, distributedId, -1) == 0) then
    redis.call('DEL', key);
    return true;
end;
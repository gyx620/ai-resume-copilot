-- Return codes: 1=reserved, 0=sold out, -1=duplicate user, -2=stock not initialized.
local user_id = ARGV[1]

if redis.call('SISMEMBER', KEYS[2], user_id) == 1 then
    return -1
end

local stock = redis.call('GET', KEYS[1])
if not stock then
    return -2
end

if tonumber(stock) <= 0 then
    return 0
end

redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], user_id)
return 1

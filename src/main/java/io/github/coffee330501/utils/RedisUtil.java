package io.github.coffee330501.utils;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.Resource;

public class RedisUtil {
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public boolean setNx(final String key, final int expireSeconds) {
        return (boolean) redisTemplate.execute((RedisCallback<Object>) connection -> {
            StringRedisSerializer serializer = new StringRedisSerializer();
            boolean success = connection.setNX(serializer.serialize(key), serializer.serialize("true"));
            if (success) {
                connection.expire(serializer.serialize(key), expireSeconds);
            }
            connection.close();
            return success;
        });
    }
}

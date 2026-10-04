package com.cocos.portfolio_service.shared.infrastructure.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SharedListCache {
    private final StringRedisTemplate redis;

    public SharedListCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void append(String key, String value) {
        redis.opsForList().rightPush(key, value);
    }

    public Long size(String key) {
        return redis.opsForList().size(key);
    }

    public List<String> range(String key, long start, long end) {
        List<String> values = redis.opsForList().range(key, start, end);
        return values == null ? List.of() : values;
    }
}

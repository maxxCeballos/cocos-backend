package com.cocos.portfolio_service.shared.infrastructure.cache;

import com.cocos.portfolio_service.shared.domain.errors.ValkeyUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SharedListCache {
    private static final Logger logger = LoggerFactory.getLogger(SharedListCache.class);
    private final StringRedisTemplate redis;

    public SharedListCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void append(String key, String value) {
        try {
            redis.opsForList().rightPush(key, value);
        } catch (DataAccessException exception) {
            throw unavailable("write", key, exception);
        }
    }

    public Long size(String key) {
        try {
            return redis.opsForList().size(key);
        } catch (DataAccessException exception) {
            throw unavailable("read size of", key, exception);
        }
    }

    public List<String> range(String key, long start, long end) {
        try {
            List<String> values = redis.opsForList().range(key, start, end);
            return values == null ? List.of() : values;
        } catch (DataAccessException exception) {
            throw unavailable("read", key, exception);
        }
    }

    private ValkeyUnavailableException unavailable(String operation, String key, DataAccessException cause) {
        logger.error("Valkey cache {} failed for key {}", operation, key, cause);
        return new ValkeyUnavailableException("Valkey cache is unavailable", cause);
    }
}

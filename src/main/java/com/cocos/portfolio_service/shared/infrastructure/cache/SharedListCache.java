package com.cocos.portfolio_service.shared.infrastructure.cache;

import com.cocos.portfolio_service.shared.domain.errors.ValkeyUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SharedListCache {
    private static final Logger logger = LoggerFactory.getLogger(SharedListCache.class);
    private static final DefaultRedisScript<Long> UPSERT_UNIQUE_LIST_ENTRY = new DefaultRedisScript<>("""
            local entries = redis.call('LRANGE', KEYS[1], 0, -1)
            local seen = {}
            local compacted = {}
            local updated = false
            for _, entry in ipairs(entries) do
                local id = string.match(entry, '"id"%s*:%s*(%d+)')
                if id then
                    if not seen[id] then
                        seen[id] = true
                        if id == ARGV[1] then
                            table.insert(compacted, ARGV[2])
                            updated = true
                        else
                            table.insert(compacted, entry)
                        end
                    end
                else
                    table.insert(compacted, entry)
                end
            end
            if not updated then
                table.insert(compacted, ARGV[2])
            end
            redis.call('DEL', KEYS[1])
            for _, entry in ipairs(compacted) do
                redis.call('RPUSH', KEYS[1], entry)
            end
            return #compacted
            """, Long.class);

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

    public void upsertById(String key, Long id, String value) {
        try {
            redis.execute(UPSERT_UNIQUE_LIST_ENTRY, List.of(key), id.toString(), value);
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

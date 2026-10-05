package com.cocos.portfolio_service.shared.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "app.valkey.startup-gate", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ValkeyStartupGate implements SmartInitializingSingleton {
    private static final Logger logger = LoggerFactory.getLogger(ValkeyStartupGate.class);

    private final StringRedisTemplate redis;
    private final Duration retryInterval;

    public ValkeyStartupGate(
            StringRedisTemplate redis,
            @Value("${app.valkey.startup-gate.retry-interval:1s}") Duration retryInterval) {
        this.redis = redis;
        this.retryInterval = retryInterval;
    }

    @Override
    public void afterSingletonsInstantiated() {
        boolean loggedUnavailable = false;
        while (true) {
            try {
                RedisCallback<String> ping = connection -> connection.ping();
                String response = redis.execute(ping);
                if ("PONG".equalsIgnoreCase(response)) {
                    if (loggedUnavailable) logger.info("Valkey is available; continuing application startup");
                    return;
                }
                if (!loggedUnavailable) {
                    logger.warn("Unexpected response to Valkey PING while waiting for startup: {}", response);
                    loggedUnavailable = true;
                } else {
                    logger.debug("Unexpected response to Valkey PING while waiting for startup: {}", response);
                }
            } catch (DataAccessException exception) {
                if (!loggedUnavailable) {
                    logger.warn("Valkey is unavailable; application startup is waiting for it", exception);
                    loggedUnavailable = true;
                } else {
                    logger.debug("Valkey is still unavailable during startup", exception);
                }
            }
            try {
                Thread.sleep(retryInterval.toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for Valkey during startup", exception);
            }
        }
    }
}

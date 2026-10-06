package com.cocos.portfolio_service.shared.infrastructure.lock;

import com.cocos.portfolio_service.shared.domain.errors.LockAcquisitionTimeoutException;
import com.cocos.portfolio_service.shared.domain.errors.ValkeyUnavailableException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SharedLockService {
    private static final Logger logger = LoggerFactory.getLogger(SharedLockService.class);
    private static final String KEY_PREFIX = "lock:user:";
    private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end", Long.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redis;
    private final Duration leaseDuration;
    private final Duration renewalInterval;
    private final Duration waitTimeout;
    private final Duration retryInterval;
    private final ScheduledExecutorService renewer = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "shared-lock-renewer");
        thread.setDaemon(true);
        return thread;
    });

    public SharedLockService(
            StringRedisTemplate redis,
            @Value("${shared.lock.lease-duration:30s}") Duration leaseDuration,
            @Value("${shared.lock.renewal-interval:10s}") Duration renewalInterval,
            @Value("${shared.lock.wait-timeout:10s}") Duration waitTimeout,
            @Value("${shared.lock.retry-interval:50ms}") Duration retryInterval) {
        this.redis = redis;
        this.leaseDuration = leaseDuration;
        this.renewalInterval = renewalInterval;
        this.waitTimeout = waitTimeout;
        this.retryInterval = retryInterval;
    }

    public LockHandle acquireForUser(Long userId) {
        String key = KEY_PREFIX + userId;
        String owner = UUID.randomUUID().toString();
        long deadline = System.nanoTime() + waitTimeout.toNanos();
        while (true) {
            try {
                Boolean acquired = redis.opsForValue().setIfAbsent(key, owner, leaseDuration);
                if (Boolean.TRUE.equals(acquired)) {
                    return scheduleRenewal(key, owner);
                }
                if (acquired == null) {
                    logger.error("Valkey returned no result while acquiring distributed lock for key {}", key);
                    throw new ValkeyUnavailableException("Valkey returned no result while acquiring a lock");
                }
            } catch (DataAccessException exception) {
                logger.error("Valkey failed while acquiring distributed lock for key {}", key, exception);
                throw new ValkeyUnavailableException("Valkey is unavailable while acquiring a lock", exception);
            }

            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw new LockAcquisitionTimeoutException(key);
            }
            try {
                TimeUnit.NANOSECONDS.sleep(Math.min(retryInterval.toNanos(), remaining));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ValkeyUnavailableException("Interrupted while waiting to acquire a lock", exception);
            }
        }
    }

    private LockHandle scheduleRenewal(String key, String owner) {
        AtomicBoolean lost = new AtomicBoolean();
        ScheduledFuture<?> renewal = renewer.scheduleAtFixedRate(() -> {
            try {
                Long renewed = redis.execute(RENEW_SCRIPT, List.of(key), owner,
                        Long.toString(leaseDuration.toMillis()));
                if (!Long.valueOf(1L).equals(renewed)) {
                    lost.set(true);
                    logger.error("Distributed lock ownership was lost for key {}", key);
                }
            } catch (DataAccessException exception) {
                lost.set(true);
                logger.error("Valkey failed while renewing distributed lock for key {}", key, exception);
            }
        }, renewalInterval.toMillis(), renewalInterval.toMillis(), TimeUnit.MILLISECONDS);
        return new LockHandle(key, owner, lost, renewal);
    }

    @PreDestroy
    void shutdownRenewer() {
        renewer.shutdownNow();
    }

    public final class LockHandle {
        private final String key;
        private final String owner;
        private final AtomicBoolean lost;
        private final ScheduledFuture<?> renewal;
        private final AtomicBoolean released = new AtomicBoolean();

        private LockHandle(String key, String owner, AtomicBoolean lost, ScheduledFuture<?> renewal) {
            this.key = key;
            this.owner = owner;
            this.lost = lost;
            this.renewal = renewal;
        }

        public void assertValid() {
            if (lost.get()) {
                throw new ValkeyUnavailableException("Distributed lock lease was lost for key " + key);
            }
        }

        public void release() {
            if (!released.compareAndSet(false, true)) return;
            renewal.cancel(false);
            try {
                Long deleted = redis.execute(RELEASE_SCRIPT, List.of(key), owner);
                if (!Long.valueOf(1L).equals(deleted)) {
                    logger.warn("Distributed lock was not released because ownership changed for key {}", key);
                }
            } catch (DataAccessException exception) {
                logger.error("Valkey failed while releasing distributed lock for key {}; lease will expire", key, exception);
            }
        }
    }
}

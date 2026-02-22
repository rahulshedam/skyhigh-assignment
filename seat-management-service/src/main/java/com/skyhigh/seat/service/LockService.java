package com.skyhigh.seat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for distributed locking using Redisson.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LockService {

    private final RedissonClient redissonClient;

    private static final long DEFAULT_WAIT_TIME = 5;
    private static final long DEFAULT_LEASE_TIME = 30;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    /**
     * Acquire a distributed lock.
     *
     * @param lockKey the lock key
     * @return RLock instance if acquired, null otherwise
     */
    public RLock acquireLock(String lockKey) {
        return acquireLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME);
    }

    /**
     * Acquire a distributed lock with custom wait and lease times.
     *
     * @param lockKey   the lock key
     * @param waitTime  maximum time to wait for the lock
     * @param leaseTime time after which the lock will be automatically released
     * @return RLock instance if acquired, null otherwise
     */
    public RLock acquireLock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, TIME_UNIT);
            if (acquired) {
                log.debug("Lock acquired: {}", lockKey);
                return lock;
            } else {
                log.warn("Failed to acquire lock: {}", lockKey);
                return null;
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while acquiring lock: {}", lockKey, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Release a distributed lock.
     *
     * @param lock the lock to release
     */
    public void releaseLock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Lock released: {}", lock.getName());
        }
    }

    /**
     * Execute a task with a distributed lock.
     *
     * @param lockKey the lock key
     * @param task    the task to execute
     * @param <T>     the return type
     * @return the result of the task
     * @throws RuntimeException if lock cannot be acquired
     */
    public <T> T executeWithLock(String lockKey, LockTask<T> task) {
        RLock lock = acquireLock(lockKey);
        if (lock == null) {
            throw new RuntimeException("Failed to acquire lock: " + lockKey);
        }

        try {
            return task.execute();
        } finally {
            releaseLock(lock);
        }
    }

    @FunctionalInterface
    public interface LockTask<T> {
        T execute();
    }
}

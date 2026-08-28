package com.example.currencyconverter.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Per-client in-JVM locks. The app runs as a single instance, so an in-process
 * lock on the client id is enough to serialize conversions per client without
 * DB row locks. A lock is created lazily per client and cached for the lifetime
 * of the process.
 * <p>
 * The {@link Lock} abstraction is deliberately used so the implementation can
 * be swapped for a distributed one (e.g. Spring Integration's
 * {@code RedisLockRegistry}) if the app ever runs more than one instance.
 * For a many-tenant production app you would also cap the map (e.g. striped
 * locks) to bound memory.
 */
@Component
public class ClientLockManager {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public Lock lockFor(String clientId) {
        return locks.computeIfAbsent(clientId, k -> new ReentrantLock());
    }
}

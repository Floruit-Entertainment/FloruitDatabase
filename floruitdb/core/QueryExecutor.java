package com.floruitdb.core;

import com.floruitdb.command.DatabaseCommand;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executes DatabaseCommand asynchronously using virtual threads, with retry, timeouts and metrics.
 */
@Slf4j
@RequiredArgsConstructor
public final class QueryExecutor {
    private final DataSource dataSource;
    private final RetryPolicy retryPolicy;
    private final long queryTimeoutSeconds;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    @Getter
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final Timer queryTimer = registry.timer("floruitdb.query.duration");
    private final Counter queryCounter = registry.counter("floruitdb.query.count");
    private final Counter queryFailures = registry.counter("floruitdb.query.failures");

    public <T> CompletableFuture<T> submit(DatabaseCommand<T> command) {
        Objects.requireNonNull(command, "command");
        return CompletableFuture.supplyAsync(() -> {
            queryCounter.increment();
            try {
                return retryPolicy.execute(() -> {
                    try (Connection conn = dataSource.getConnection()) {
                        conn.setNetworkTimeout(null, (int) (queryTimeoutSeconds * 1000));
                        return queryTimer.recordCallable(() -> command.execute(conn));
                    }
                });
            } catch (Exception e) {
                queryFailures.increment();
                log.error("Query execution failed", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public void shutdown() {
        try {
            executor.shutdownNow();
        } catch (Exception ignored) {
        }
    }

}

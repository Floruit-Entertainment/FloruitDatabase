package com.floruitdb.core;

import com.floruitdb.config.DataSourceProvider;
import com.floruitdb.config.DatabaseConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.Objects;

/**
 * Main API for FloruitDB. Manages lifecycle and exposes executor.
 */
@Slf4j
public final class FloruitDB implements Closeable {
    @Getter
    private final DatabaseConfig config;
    private final DataSourceProvider provider;
    @Getter
    private final QueryExecutor executor;

    public FloruitDB(DatabaseConfig config) {
        Objects.requireNonNull(config, "config");
        this.config = config;
        this.provider = new DataSourceProvider(config);
        RetryPolicy retry = RetryPolicy.builder()
                .maxAttempts(config.maxRetries())
                .initialDelayMillis(config.retryBackoff().toMillis())
                .multiplier(2.0)
                .build();
        this.executor = new QueryExecutor(
                provider.getDataSource(),
                retry,
                config.queryTimeout().getSeconds()
        );
    }

    @Override
    public void close() {
        try {
            executor.shutdown();
        } catch (Exception ignored) {
        }
        try {
            provider.close();
        } catch (Exception ignored) {
        }
        log.info("FloruitDB closed");
    }
}

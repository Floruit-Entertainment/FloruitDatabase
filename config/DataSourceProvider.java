package com.floruitdb.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * Provides and manages HikariDataSource lifecycle.
 */
@RequiredArgsConstructor
public final class DataSourceProvider {
    private final DatabaseConfig config;
    private volatile HikariDataSource dataSource;

    public HikariDataSource getDataSource() {
        Objects.requireNonNull(config, "config");
        HikariDataSource ds = dataSource;
        if (ds != null && !ds.isClosed()) return ds;

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(config.jdbcUrl());
        hc.setUsername(config.username());
        hc.setPassword(config.password());
        hc.setMaximumPoolSize(config.maxPoolSize());
        hc.setConnectionTimeout(config.connectionTimeout().toMillis());

        dataSource = new HikariDataSource(hc);
        return dataSource;
    }

    public void close() {
        HikariDataSource ds = dataSource;
        if (ds == null) return;
        try {
            ds.close();
        } catch (Exception ignored) {
        }
    }
}

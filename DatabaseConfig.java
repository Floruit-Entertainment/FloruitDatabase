package com.floruitdb.config;

import lombok.Builder;
import lombok.NonNull;

import java.time.Duration;

/**
 * Immutable DB configuration.
 */
@Builder
public record DatabaseConfig(
        @NonNull String jdbcUrl,
        @NonNull String username,
        @NonNull String password,
        int maxPoolSize,
        Duration connectionTimeout,
        Duration queryTimeout,
        int maxRetries,
        Duration retryBackoff
) {
    public static DatabaseConfig defaultValues(@NonNull String jdbcUrl,
                                               @NonNull String username,
                                               @NonNull String password,
                                               int maxPoolSize,
                                               Duration connectionTimeout,
                                               Duration queryTimeout,
                                               int maxRetries,
                                               Duration retryBackoff) {
        return new DatabaseConfig(
                jdbcUrl,
                username,
                password,
                maxPoolSize <= 0 ? 10 : maxPoolSize,
                connectionTimeout == null ? Duration.ofSeconds(10) : connectionTimeout,
                queryTimeout == null ? Duration.ofSeconds(5) : queryTimeout,
                maxRetries < 0 ? 3 : maxRetries,
                retryBackoff == null ? Duration.ofMillis(200) : retryBackoff
        );
    }
}

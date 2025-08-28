package com.floruitdb.core;

import lombok.Builder;

import java.util.concurrent.Callable;

/**
 * Simple retry policy with exponential backoff multiplier.
 */
@Builder
public record RetryPolicy(int maxAttempts, long initialDelayMillis, double multiplier) {
    public <T> T execute(Callable<T> callable) throws Exception {
        long delay = initialDelayMillis;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callable.call();
            } catch (Exception ex) {
                if (attempt == maxAttempts) throw ex;
                Thread.sleep(delay);
                delay = (long) (delay * multiplier);
            }
        }
        throw new IllegalStateException("RetryPolicy failed unexpectedly");
    }
}

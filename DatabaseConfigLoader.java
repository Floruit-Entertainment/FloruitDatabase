package com.floruitdb.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads DB config from properties resource.
 */
public final class DatabaseConfigLoader {

    private DatabaseConfigLoader() {
    }

    public static DatabaseConfig fromProperties(String resource) {
        Objects.requireNonNull(resource, "resource");
        Properties props = new Properties();
        try (InputStream in = DatabaseConfigLoader.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalArgumentException("Resource not found: " + resource);
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties: " + resource, e);
        }

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String pass = props.getProperty("db.pass");
        int pool = Integer.parseInt(props.getProperty("db.pool", "10"));
        int timeoutSec = Integer.parseInt(props.getProperty("db.queryTimeoutSeconds", "5"));
        int maxRetries = Integer.parseInt(props.getProperty("db.maxRetries", "3"));
        long backoffMillis = Long.parseLong(props.getProperty("db.retryBackoffMillis", "200"));

        return DatabaseConfig.defaultValues(
                url,
                user,
                pass,
                pool,
                Duration.ofSeconds(10),
                Duration.ofSeconds(timeoutSec),
                maxRetries,
                Duration.ofMillis(backoffMillis)
        );
    }
}

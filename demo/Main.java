package com.floruitdb.demo;

import com.floruitdb.command.BatchUpdateCommand;
import com.floruitdb.command.QueryCommand;
import com.floruitdb.config.DatabaseConfig;
import com.floruitdb.config.DatabaseConfigLoader;
import com.floruitdb.core.FloruitDB;
import com.floruitdb.core.RecordMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Example usage of FloruitDB.
 */
@Slf4j
public class Main {
    public static void main(String[] args) {
        DatabaseConfig config = DatabaseConfigLoader.fromProperties("/db.properties");
        try (FloruitDB db = new FloruitDB(config)) {
            var futUser = db.getExecutor().submit(
                    QueryCommand.of(
                            "SELECT id, name, active FROM users WHERE id = ?",
                            rs -> RecordMapper.map(rs, User.class),
                            1
                    )
            );

            var futBatch = db.getExecutor().submit(
                    BatchUpdateCommand.of(
                            "INSERT INTO logs (msg, ts) VALUES (?, ?)",
                            List.of(
                                    List.of("first", System.currentTimeMillis()),
                                    List.of("second", System.currentTimeMillis())
                            )
                    )
            );

            log.info("User: {}", futUser.join());
            log.info("Batch result: {}", java.util.Arrays.toString(futBatch.join()));
        } catch (Exception e) {
            log.error("Application error", e);
        }
    }

    public record User(int id, String name, boolean active) {
    }
}

package com.floruitdb.command;

import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Batch update command.
 */
@RequiredArgsConstructor(staticName = "of")
public final class BatchUpdateCommand implements DatabaseCommand<int[]> {

    private final String sql;
    private final List<List<Object>> batchParams;

    public static BatchUpdateCommand of(String sql, List<List<Object>> batchParams) {
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(batchParams, "batchParams");
        return new BatchUpdateCommand(sql, batchParams);
    }

    @Override
    public int[] execute(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (List<Object> row : batchParams) {
                for (int i = 0; i < row.size(); i++) stmt.setObject(i + 1, row.get(i));
                stmt.addBatch();
            }
            return stmt.executeBatch();
        }
    }
}

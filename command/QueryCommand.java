package com.floruitdb.command;

import lombok.RequiredArgsConstructor;

import java.sql.ResultSet;
import java.util.Objects;
import java.util.function.Function;

/**
 * Query command that maps a ResultSet to T.
 */
@RequiredArgsConstructor
public final class QueryCommand<T> implements DatabaseCommand<T> {
    private final String sql;
    private final Function<ResultSet, T> mapper;
    private final Object[] params;

    // Keep your manual 'of' method for the extra logic
    public static <T> QueryCommand<T> of(String sql, Function<ResultSet, T> mapper, Object... params) {
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(mapper, "mapper");
        return new QueryCommand<>(sql, mapper, params == null ? new Object[0] : params);
    }

    @Override
    public T execute(java.sql.Connection connection) throws java.sql.SQLException {
        try (var stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            boolean hasResult = stmt.execute();
            if (!hasResult) {
                return null;
            }
            try (ResultSet rs = stmt.getResultSet()) {
                return mapper.apply(rs);
            }
        }
    }
}
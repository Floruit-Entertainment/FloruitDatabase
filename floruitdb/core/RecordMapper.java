package com.floruitdb.core;

import lombok.SneakyThrows;

import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Maps a ResultSet (current row) to a record instance by matching column names to component names.
 */
public final class RecordMapper {
    private RecordMapper() {
    }

    @SneakyThrows
    public static <T extends Record> T map(ResultSet rs, Class<T> type) throws SQLException {
        Objects.requireNonNull(rs, "rs");
        Objects.requireNonNull(type, "type");

        if (!rs.next()) return null;

        RecordComponent[] components = type.getRecordComponents();
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            values[i] = rs.getObject(components[i].getName());
        }

        Class<?>[] paramTypes = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) paramTypes[i] = components[i].getType();

        return type.getDeclaredConstructor(paramTypes).newInstance(values);
    }
}

package com.floruitdb.command;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Represents an operation executed with a JDBC Connection.
 */
@FunctionalInterface
public interface DatabaseCommand<T> {
    T execute(Connection connection) throws SQLException;
}

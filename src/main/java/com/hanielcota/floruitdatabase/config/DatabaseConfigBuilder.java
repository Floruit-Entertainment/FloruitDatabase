package com.hanielcota.floruitdatabase.config;

import lombok.NonNull;

/**
 * Implementação do Design Pattern 'Builder' para o record {@link DatabaseConfig}.
 *
 * <p>Fornece uma API fluente e legível para construir a configuração do banco de dados.
 */
public class DatabaseConfigBuilder {

  private final String host;
  private final String databaseName;
  private final String username;
  private int port = 3306;
  private String password = "";

  public DatabaseConfigBuilder(@NonNull String host, @NonNull String databaseName, @NonNull String username) {
    this.host = host;
    this.databaseName = databaseName;
    this.username = username;
  }

  public DatabaseConfigBuilder port(int port) {
    this.port = port;
    return this;
  }

  public DatabaseConfigBuilder password(String password) {
    this.password = (password != null) ? password : "";
    return this;
  }

  public DatabaseConfig build() {
    return new DatabaseConfig(host, port, databaseName, username, password);
  }
}

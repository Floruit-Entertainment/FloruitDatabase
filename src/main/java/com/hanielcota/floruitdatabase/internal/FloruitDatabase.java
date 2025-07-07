package com.hanielcota.floruitdatabase.internal;

import com.hanielcota.floruitdatabase.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe interna que gerencia o pool de conexões e a execução de todos os comandos SQL.
 *
 * <p>Esta classe é o "motor" por trás da fachada {@link
 * com.hanielcota.floruitdatabase.FloruitDatabaseAPI}, encapsulando toda a lógica de baixo nível do
 * JDBC e do pool de conexões HikariCP. Ela não deve ser usada diretamente pelo consumidor da API.
 *
 * @see com.hanielcota.floruitdatabase.FloruitDB
 */
public class FloruitDatabase {

  private static final Logger log = LoggerFactory.getLogger(FloruitDatabase.class);

  private final ExecutorService virtualThreadExecutor;
  private final HikariDataSource dataSource;

  /**
   * Constrói o executor de banco de dados.
   *
   * <p>Este construtor inicializa o pool de conexões HikariCP com base na configuração fornecida.
   * Lançará uma {@link RuntimeException} se a conexão com o banco de dados falhar.
   *
   * @param config O objeto de configuração do banco de dados, não pode ser nulo.
   * @param virtualThreadExecutor O executor de threads virtuais compartilhado para operações
   *     assíncronas, não pode ser nulo.
   */
  public FloruitDatabase(@NonNull DatabaseConfig config, @NonNull ExecutorService virtualThreadExecutor) {
    this.virtualThreadExecutor = virtualThreadExecutor;
    try {
      this.dataSource = createDataSource(config);
      log.info("Pool de conexões com o banco de dados criado com sucesso.");
    } catch (Exception e) {
      log.error(
          "Falha crítica ao criar o pool de conexões HikariCP. Verifique as credenciais e a conectividade.",
          e);
      throw new RuntimeException("Não foi possível inicializar o banco de dados.", e);
    }
  }

  /** Fecha o pool de conexões do HikariCP, liberando todos os recursos. */
  public void close() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      log.info("Pool de conexões com o banco de dados foi fechado.");
    }
  }

  /**
   * Verifica se o pool de conexões está ativo e pronto para uso.
   *
   * @return {@code true} se o dataSource estiver inicializado e não fechado, {@code false} caso
   *     contrário.
   */
  boolean isConnected() {
    return dataSource != null && !dataSource.isClosed();
  }

  /**
   * Executa uma operação de atualização (INSERT, UPDATE, DELETE) de forma assíncrona.
   *
   * @param sql A instrução SQL a ser executada. Não pode ser nula.
   * @param params Os parâmetros para a instrução.
   * @return um {@link CompletableFuture} que será concluído com o número de linhas afetadas.
   */
  public CompletableFuture<Integer> executeUpdate(@NonNull String sql, Object... params) {
    if (!isConnected()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Banco de dados não está conectado."));
    }

    return CompletableFuture.supplyAsync(
        () -> {
          try (Connection connection = dataSource.getConnection();
              PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, params);
            return statement.executeUpdate();

          } catch (SQLException e) {
            log.error("Falha ao executar update [SQL: {}]", sql, e);
            // Propaga a exceção de forma padrão para CompletableFutures.
            throw new CompletionException(e);
          }
        },
        virtualThreadExecutor);
  }

  /**
   * Executa uma consulta (SELECT) e mapeia o resultado de forma assíncrona.
   *
   * @param sql A instrução SQL de consulta. Não pode ser nula.
   * @param handler A função que transforma o ResultSet em um objeto. Não pode ser nula.
   * @param <T> O tipo do objeto de retorno.
   * @param params Os parâmetros para a consulta.
   * @return um {@link CompletableFuture} que será concluído com o objeto mapeado.
   */
  public <T> CompletableFuture<T> executeQuery(
      @NonNull String sql, @NonNull Function<ResultSet, T> handler, Object... params) {
    if (!isConnected()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Banco de dados não está conectado."));
    }

    return CompletableFuture.supplyAsync(
        () -> {
          try (Connection connection = dataSource.getConnection();
              PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, params);

            try (ResultSet resultSet = statement.executeQuery()) {
              return handler.apply(resultSet);
            }
          } catch (SQLException e) {
            log.error("Falha ao executar query [SQL: {}]", sql, e);
            throw new CompletionException(e);
          }
        },
        virtualThreadExecutor);
  }

  /**
   * Executa múltiplas operações de atualização em um único lote (batch) de forma assíncrona.
   *
   * @param sql A instrução SQL a ser executada para cada entrada no lote. Não pode ser nula.
   * @param paramsList Uma lista de arrays de parâmetros. Não pode ser nula.
   * @return um {@link CompletableFuture} que será concluído com um array de inteiros (linhas
   *     afetadas por operação).
   */
  public CompletableFuture<int[]> executeBatch(
      @NonNull String sql, @NonNull List<Object[]> paramsList) {
    if (!isConnected()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Banco de dados não está conectado."));
    }
    if (paramsList.isEmpty()) {
      return CompletableFuture.completedFuture(new int[0]);
    }

    return CompletableFuture.supplyAsync(
        () -> {
          try (Connection connection = dataSource.getConnection();
              PreparedStatement statement = connection.prepareStatement(sql)) {

            for (Object[] params : paramsList) {
              setParameters(statement, params);
              statement.addBatch();
            }
            return statement.executeBatch();

          } catch (SQLException e) {
            log.error("Falha ao executar batch [SQL: {}]", sql, e);
            throw new CompletionException(e);
          }
        },
        virtualThreadExecutor);
  }

  /**
   * Executa um conjunto de operações dentro de uma transação atômica de forma assíncrona.
   *
   * @param actions O bloco de código que executa as operações. Não pode ser nulo.
   * @return um {@link CompletableFuture} que é concluído quando a transação termina.
   */
  public CompletableFuture<Void> executeInTransaction(@NonNull Consumer<Connection> actions) {
    if (!isConnected()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Banco de dados não está conectado."));
    }

    return CompletableFuture.runAsync(
        () -> {
          try (Connection connection = dataSource.getConnection()) {
            try {
              connection.setAutoCommit(false);
              actions.accept(connection);
              connection.commit();

            } catch (Exception e) {
              log.warn("Transação falhou, executando rollback.", e);
              connection.rollback();
              throw new CompletionException("A transação falhou e foi revertida.", e);
            }
          } catch (SQLException e) {
            log.error("Não foi possível obter conexão para a transação ou executar o rollback.", e);
            throw new CompletionException(e);
          }
        },
        virtualThreadExecutor);
  }

  /**
   * Utilitário privado para definir os parâmetros em um PreparedStatement.
   *
   * @param statement O statement a ser preparado.
   * @param params Os parâmetros a serem definidos.
   * @throws SQLException se ocorrer um erro de acesso ao banco de dados.
   */
  private void setParameters(PreparedStatement statement, Object... params) throws SQLException {
    if (params == null || params.length == 0) {
      return;
    }
    for (int i = 0; i < params.length; i++) {
      statement.setObject(i + 1, params[i]);
    }
  }

  /** Cria e configura a fonte de dados HikariCP. */
  private HikariDataSource createDataSource(DatabaseConfig config) {
    Objects.requireNonNull(config.host(), "O host do banco de dados não pode ser nulo.");
    Objects.requireNonNull(config.databaseName(), "O nome do banco de dados não pode ser nulo.");
    Objects.requireNonNull(config.username(), "O nome de usuário do banco de dados não pode ser nulo.");

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s", config.host(), config.port(), config.databaseName()));
    hikariConfig.setUsername(config.username());
    hikariConfig.setPassword(config.password());

    // Otimizações padrão
    hikariConfig.setMaximumPoolSize(10);
    hikariConfig.setMinimumIdle(5);
    hikariConfig.setConnectionTimeout(30000);
    hikariConfig.setLeakDetectionThreshold(15000);
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

    return new HikariDataSource(hikariConfig);
  }
}

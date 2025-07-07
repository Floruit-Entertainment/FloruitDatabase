package com.hanielcota.floruitdatabase;

import com.hanielcota.floruitdatabase.config.DatabaseConfig;
import com.hanielcota.floruitdatabase.config.DatabaseConfigBuilder;
import com.hanielcota.floruitdatabase.internal.DatabaseTaskQueue;
import com.hanielcota.floruitdatabase.internal.FloruitDatabase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A implementação concreta e ponto de entrada da biblioteca FloruitDB. O consumidor deve instanciar
 * esta classe para ter acesso à API.
 */
public final class FloruitDB implements FloruitDatabaseAPI {

  private static final Logger log = LoggerFactory.getLogger(FloruitDB.class);
  private final ExecutorService virtualThreadExecutor;
  private final FloruitDatabase database;
  private final DatabaseTaskQueue taskQueue;
  private volatile boolean isShutdown = false;

  /**
   * Constrói uma nova instância da API FloruitDB.
   *
   * @param config O objeto de configuração do banco de dados, criado via {@link
   *     DatabaseConfigBuilder}.
   */
  public FloruitDB(@NonNull DatabaseConfig config) {
    this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    this.database = new FloruitDatabase(config, this.virtualThreadExecutor);
    this.taskQueue = new DatabaseTaskQueue(this.virtualThreadExecutor);
    this.taskQueue.start(10, TimeUnit.SECONDS);
  }

  @Override
  public CompletableFuture<Integer> executeUpdate(String sql, Object... params) {
    if (isShutdown)
      return CompletableFuture.failedFuture(new IllegalStateException("API já foi finalizada."));
    return database.executeUpdate(sql, params);
  }

  @Override
  public <T> CompletableFuture<T> executeQuery(
      String sql, Function<ResultSet, T> handler, Object... params) {
    if (isShutdown)
      return CompletableFuture.failedFuture(new IllegalStateException("API já foi finalizada."));
    return database.executeQuery(sql, handler, params);
  }

  @Override
  public CompletableFuture<int[]> executeBatch(String sql, List<Object[]> paramsList) {
    if (isShutdown)
      return CompletableFuture.failedFuture(new IllegalStateException("API já foi finalizada."));
    return database.executeBatch(sql, paramsList);
  }

  @Override
  public CompletableFuture<Void> executeInTransaction(Consumer<Connection> actions) {
    if (isShutdown)
      return CompletableFuture.failedFuture(new IllegalStateException("API já foi finalizada."));
    return database.executeInTransaction(actions);
  }

  @Override
  public void submitTask(Runnable task) {
    if (isShutdown) {
      log.warn("API finalizada. Tarefa não será submetida.");
      return;
    }
    taskQueue.submit(task);
  }

  @Override
  public void shutdown() {
    if (isShutdown) return;
    synchronized (this) {
      if (isShutdown) return;
      isShutdown = true;
    }

    log.info("Iniciando o processo de finalização da FloruitDB...");
    try {
      taskQueue.shutdown();
      database.close();

      log.info("Finalizando o pool de threads virtuais...");
      virtualThreadExecutor.shutdown();
      if (!virtualThreadExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
        virtualThreadExecutor.shutdownNow();
      }

    } catch (InterruptedException e) {
      log.error("O processo de finalização foi interrompido.", e);
      Thread.currentThread().interrupt();
    }
    log.info("FloruitDB finalizada com sucesso.");
  }
}

package com.hanielcota.floruitdatabase;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A interface pública (Facade) para a biblioteca FloruitDB.
 *
 * <p>Define o contrato para interações com o banco de dados e gerência seu próprio ciclo de vida,
 * devendo ser fechada com {@link #shutdown()} ou usando um bloco try-with-resources. Todas as
 * operações são executadas de forma assíncrona e altamente escalável utilizando um pool de Threads
 * Virtuais (Project Loom).
 */
public interface FloruitDatabaseAPI extends Closeable {

  /**
   * Executa uma operação de atualização (INSERT, UPDATE, DELETE) no banco de dados.
   *
   * @param sql A instrução SQL a ser executada, com '?' para parâmetros.
   * @param params Os objetos a serem inseridos nos parâmetros da instrução SQL.
   * @return um {@link CompletableFuture} que será concluído com o número de linhas afetadas.
   */
  CompletableFuture<Integer> executeUpdate(String sql, Object... params);

  /**
   * Executa uma consulta (SELECT) no banco de dados e mapeia o resultado.
   *
   * @param sql A instrução SQL de consulta.
   * @param handler Uma função que recebe um {@link ResultSet} e o transforma em um objeto do tipo
   *     T.
   * @param <T> O tipo do objeto de retorno.
   * @param params Os parâmetros para a consulta.
   * @return um {@link CompletableFuture} que será concluído com o objeto mapeado pelo handler.
   */
  <T> CompletableFuture<T> executeQuery(
      String sql, Function<ResultSet, T> handler, Object... params);

  /**
   * Executa múltiplas operações de atualização em um único lote (batch).
   *
   * @param sql A instrução SQL a ser executada para cada entrada no lote.
   * @param paramsList Uma lista de arrays de objetos, onde cada array representa os parâmetros para
   *     uma operação.
   * @return um {@link CompletableFuture} que será concluído com um array de inteiros, representando
   *     as linhas afetadas por cada operação no lote.
   */
  CompletableFuture<int[]> executeBatch(String sql, List<Object[]> paramsList);

  /**
   * Executa um conjunto de operações dentro de uma transação atômica.
   *
   * @param actions um {@link Consumer} que recebe uma {@link Connection} para executar as operações
   *     da transação.
   * @return um {@link CompletableFuture} que será concluído quando a transação for finalizada (com
   *     commit ou rollback).
   */
  CompletableFuture<Void> executeInTransaction(Consumer<Connection> actions);

  /**
   * Submete uma tarefa para ser executada na fila sequencial de banco de dados (Command Pattern).
   *
   * @param task A tarefa (Comando) a ser enfileirada e executada.
   */
  void submitTask(Runnable task);

  /**
   * Finaliza todos os serviços da API de forma graciosa.
   *
   * <p><b>É crucial chamar este método quando a aplicação for finalizada para evitar vazamento de
   * recursos.</b>
   */
  void shutdown();

  /** Permite que a API seja usada em blocos try-with-resources. */
  @Override
  default void close() {
    shutdown();
  }
}

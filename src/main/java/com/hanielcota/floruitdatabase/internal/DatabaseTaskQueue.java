package com.hanielcota.floruitdatabase.internal;

import java.util.Queue;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe interna que gerencia a fila de comandos. Não deve ser usada diretamente pelo consumidor.
 */
public class DatabaseTaskQueue {

  private static final Logger log = LoggerFactory.getLogger(DatabaseTaskQueue.class);
  private final ExecutorService virtualThreadExecutor;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

  public DatabaseTaskQueue(ExecutorService virtualThreadExecutor) {
    this.virtualThreadExecutor = virtualThreadExecutor;
  }

  public void start(long interval, TimeUnit unit) {
    scheduler.scheduleAtFixedRate(this::processQueue, interval, interval, unit);
  }

  public void submit(Runnable task) {
    taskQueue.add(task);
  }

  private void processQueue() {
    Runnable task;
    while ((task = taskQueue.poll()) != null) {
      try {
        virtualThreadExecutor.execute(task);
      } catch (Exception e) {
        log.error("Erro ao submeter tarefa da fila para o executor.", e);
      }
    }
  }

  public void shutdown() throws InterruptedException {
    log.debug("Finalizando o agendador da fila de tarefas...");
    scheduler.shutdown();
    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
      scheduler.shutdownNow();
    }
    log.debug("Processando itens restantes da fila antes de desligar...");
    processQueue();
  }
}

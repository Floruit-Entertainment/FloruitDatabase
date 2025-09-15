package com.hanielcota.floruitdatabase.core;

import com.hanielcota.floruitdatabase.command.DatabaseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fila de comandos para execução sequencial e assíncrona.
 * 
 * <p>Esta classe implementa uma fila de comandos que permite enfileirar
 * operações de banco de dados para execução sequencial, garantindo ordem
 * e controle de concorrência.
 * 
 * <p>Utiliza Virtual Threads para processamento assíncrono e oferece
 * métricas de performance e controle de fluxo.
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public final class CommandQueue implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(CommandQueue.class);
    
    private final BlockingQueue<QueuedCommand<?>> commandQueue;
    private final ExecutorService virtualThreadExecutor;
    private final ScheduledExecutorService scheduler;
    private final ConnectionManager connectionManager;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong processedCommands = new AtomicLong(0);
    private final AtomicLong failedCommands = new AtomicLong(0);
    
    private volatile boolean closed = false;
    
    /**
     * Constrói uma nova fila de comandos.
     * 
     * @param queueCapacity Capacidade máxima da fila
     * @param connectionManager Gerenciador de conexões para executar comandos
     */
    public CommandQueue(int queueCapacity, ConnectionManager connectionManager) {
        this.commandQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.connectionManager = connectionManager;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "CommandQueue-Scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * Inicia o processamento da fila de comandos.
     * 
     * @param processingInterval Intervalo entre processamentos em milissegundos
     */
    public void start(long processingInterval) {
        if (running.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(
                this::processQueue,
                0,
                processingInterval,
                TimeUnit.MILLISECONDS
            );
            logger.info("Fila de comandos iniciada com intervalo de {}ms", processingInterval);
        }
    }
    
    /**
     * Enfileira um comando para execução assíncrona.
     * 
     * @param command Comando a ser enfileirado
     * @param <T> Tipo de retorno do comando
     * @return CompletableFuture que será completado com o resultado do comando
     */
    public <T> CompletableFuture<T> enqueue(DatabaseCommand<T> command) {
        if (closed) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Fila de comandos foi fechada"));
        }
        
        CompletableFuture<T> future = new CompletableFuture<>();
        QueuedCommand<T> queuedCommand = new QueuedCommand<>(command, future);
        
        try {
            boolean added = commandQueue.offer(queuedCommand, 5, TimeUnit.SECONDS);
            if (!added) {
                future.completeExceptionally(
                    new RuntimeException("Fila de comandos está cheia"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Enfileira um comando com timeout personalizado.
     * 
     * @param command Comando a ser enfileirado
     * @param timeout Timeout para adicionar à fila
     * @param unit Unidade de tempo do timeout
     * @param <T> Tipo de retorno do comando
     * @return CompletableFuture que será completado com o resultado do comando
     */
    public <T> CompletableFuture<T> enqueue(DatabaseCommand<T> command, long timeout, TimeUnit unit) {
        if (closed) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Fila de comandos foi fechada"));
        }
        
        CompletableFuture<T> future = new CompletableFuture<>();
        QueuedCommand<T> queuedCommand = new QueuedCommand<>(command, future);
        
        try {
            boolean added = commandQueue.offer(queuedCommand, timeout, unit);
            if (!added) {
                future.completeExceptionally(
                    new RuntimeException("Timeout ao adicionar comando à fila"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Processa comandos da fila.
     */
    private void processQueue() {
        if (!running.get() || closed) {
            return;
        }
        
        QueuedCommand<?> queuedCommand;
        while ((queuedCommand = commandQueue.poll()) != null) {
            virtualThreadExecutor.execute(() -> {
                try (Connection connection = connectionManager.getConnection()) {
                    // Executa o comando com uma conexão do pool
                    Object result = queuedCommand.getCommand().execute(connection).join();
                    queuedCommand.complete(result);
                    processedCommands.incrementAndGet();
                    
                } catch (Exception e) {
                    queuedCommand.completeExceptionally(e);
                    failedCommands.incrementAndGet();
                    logger.error("Erro ao processar comando: {}", queuedCommand.getCommand().getDescription(), e);
                }
            });
        }
    }
    
    /**
     * Retorna informações sobre o estado da fila.
     * 
     * @return Informações da fila
     */
    public QueueInfo getInfo() {
        return new QueueInfo(
            commandQueue.size(),
            commandQueue.remainingCapacity(),
            processedCommands.get(),
            failedCommands.get(),
            running.get() && !closed
        );
    }
    
    /**
     * Fecha a fila de comandos de forma graciosa.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        running.set(false);
        
        logger.info("Fechando fila de comandos...");
        
        // Para o scheduler
        scheduler.shutdown();
        
        // Processa comandos restantes
        processQueue();
        
        // Fecha o executor
        virtualThreadExecutor.shutdown();
        
        try {
            if (!virtualThreadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                virtualThreadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            virtualThreadExecutor.shutdownNow();
        }
        
        logger.info("Fila de comandos fechada. Processados: {}, Falharam: {}", 
                   processedCommands.get(), failedCommands.get());
    }
    
    /**
     * Classe interna para representar um comando enfileirado.
     */
    private static class QueuedCommand<T> {
        private final DatabaseCommand<T> command;
        private final CompletableFuture<T> future;
        
        public QueuedCommand(DatabaseCommand<T> command, CompletableFuture<T> future) {
            this.command = command;
            this.future = future;
        }
        
        public DatabaseCommand<T> getCommand() {
            return command;
        }
        
        @SuppressWarnings("unchecked")
        public void complete(Object result) {
            future.complete((T) result);
        }
        
        public void completeExceptionally(Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }
    
    /**
     * Classe para encapsular informações sobre o estado da fila.
     */
    public record QueueInfo(
        int queueSize,
        int remainingCapacity,
        long processedCommands,
        long failedCommands,
        boolean isRunning
    ) {
        /**
         * Calcula a taxa de sucesso dos comandos processados.
         * 
         * @return Taxa de sucesso (0.0 a 1.0)
         */
        public double getSuccessRate() {
            long total = processedCommands + failedCommands;
            return total > 0 ? (double) processedCommands / total : 1.0;
        }
        
        /**
         * Verifica se a fila está próxima da capacidade máxima.
         * 
         * @return true se a utilização for maior que 80%
         */
        public boolean isNearCapacity() {
            int totalCapacity = queueSize + remainingCapacity;
            return totalCapacity > 0 && (double) queueSize / totalCapacity > 0.8;
        }
    }
}

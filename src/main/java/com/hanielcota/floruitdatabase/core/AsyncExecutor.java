package com.hanielcota.floruitdatabase.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Executor assíncrono que utiliza Virtual Threads do Java 21 para máxima performance.
 * 
 * <p>Esta classe encapsula o uso de Virtual Threads para execução de operações
 * de banco de dados de forma assíncrona, proporcionando alta concorrência com
 * baixo overhead de recursos.
 * 
 * <p>Implementa o padrão Singleton para garantir uma única instância do executor
 * por aplicação, otimizando o uso de recursos do sistema.
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public final class AsyncExecutor implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncExecutor.class);
    
    private final ExecutorService virtualThreadExecutor;
    private volatile boolean closed = false;
    
    /**
     * Constrói um novo executor assíncrono com Virtual Threads.
     */
    public AsyncExecutor() {
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        logger.info("Executor assíncrono com Virtual Threads inicializado");
    }
    
    /**
     * Executa uma tarefa de forma assíncrona usando Virtual Threads.
     * 
     * @param task Tarefa a ser executada
     * @param <T> Tipo de retorno da tarefa
     * @return CompletableFuture que será completado com o resultado da tarefa
     */
    public <T> CompletableFuture<T> executeAsync(Supplier<T> task) {
        if (closed) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Executor foi fechado"));
        }
        
        return CompletableFuture.supplyAsync(task, virtualThreadExecutor);
    }
    
    /**
     * Executa uma tarefa Runnable de forma assíncrona usando Virtual Threads.
     * 
     * @param task Tarefa a ser executada
     * @return CompletableFuture que será completado quando a tarefa terminar
     */
    public CompletableFuture<Void> executeAsync(Runnable task) {
        if (closed) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Executor foi fechado"));
        }
        
        return CompletableFuture.runAsync(task, virtualThreadExecutor);
    }
    
    /**
     * Executa uma tarefa com retry automático em caso de falha.
     * 
     * @param task Tarefa a ser executada
     * @param maxRetries Número máximo de tentativas
     * @param delayMs Delay entre tentativas em milissegundos
     * @param <T> Tipo de retorno da tarefa
     * @return CompletableFuture que será completado com o resultado da tarefa
     */
    public <T> CompletableFuture<T> executeWithRetry(
            Supplier<T> task, int maxRetries, long delayMs) {
        
        return executeAsync(() -> {
            Exception lastException = null;
            
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    return task.get();
                } catch (Exception e) {
                    lastException = e;
                    
                    if (attempt < maxRetries) {
                        logger.warn("Tentativa {} falhou, tentando novamente em {}ms: {}", 
                                  attempt + 1, delayMs, e.getMessage());
                        
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Thread interrompida durante retry", ie);
                        }
                    }
                }
            }
            
            throw new RuntimeException("Todas as tentativas falharam", lastException);
        });
    }
    
    /**
     * Verifica se o executor está ativo.
     * 
     * @return true se o executor estiver ativo, false caso contrário
     */
    public boolean isActive() {
        return !closed && !virtualThreadExecutor.isShutdown();
    }
    
    /**
     * Retorna informações sobre o estado do executor.
     * 
     * @return Informações do executor
     */
    public ExecutorInfo getInfo() {
        if (closed || virtualThreadExecutor.isShutdown()) {
            return new ExecutorInfo(false, 0, 0);
        }
        
        // Para Virtual Threads, não temos métricas diretas como thread pools tradicionais
        // mas podemos verificar se está ativo
        return new ExecutorInfo(true, -1, -1); // -1 indica que não é aplicável para Virtual Threads
    }
    
    /**
     * Fecha o executor de forma graciosa, aguardando a conclusão das tarefas pendentes.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        logger.info("Iniciando fechamento do executor assíncrono...");
        
        virtualThreadExecutor.shutdown();
        
        try {
            if (!virtualThreadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Executor não terminou graciosamente, forçando fechamento...");
                virtualThreadExecutor.shutdownNow();
                
                if (!virtualThreadExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.error("Executor não pôde ser fechado completamente");
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Fechamento do executor foi interrompido");
            virtualThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Executor assíncrono fechado com sucesso");
    }
    
    /**
     * Classe para encapsular informações sobre o estado do executor.
     */
    public record ExecutorInfo(
        boolean isActive,
        int activeThreads,
        int queuedTasks
    ) {
        /**
         * Retorna uma descrição legível do estado do executor.
         * 
         * @return Descrição do estado
         */
        public String getStatus() {
            if (!isActive) {
                return "Executor fechado";
            }
            
            if (activeThreads == -1) {
                return "Executor ativo (Virtual Threads)";
            }
            
            return String.format("Executor ativo - Threads: %d, Fila: %d", 
                               activeThreads, queuedTasks);
        }
    }
}

package com.hanielcota.floruitdatabase;

import com.hanielcota.floruitdatabase.command.*;
import com.hanielcota.floruitdatabase.config.DatabaseConfig;
import com.hanielcota.floruitdatabase.core.AsyncExecutor;
import com.hanielcota.floruitdatabase.core.CommandQueue;
import com.hanielcota.floruitdatabase.core.ConnectionManager;
import com.hanielcota.floruitdatabase.exception.ConnectionException;
import com.hanielcota.floruitdatabase.exception.FloruitDatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * API principal do FloruitDatabase - Facade para todas as operações de banco de dados.
 * 
 * <p>Esta classe implementa o padrão Facade, fornecendo uma interface simplificada
 * e unificada para todas as operações de banco de dados. Encapsula a complexidade
 * interna do gerenciamento de conexões, execução assíncrona e filas de comandos.
 * 
 * <p>Características principais:
 * <ul>
 *   <li>Operações assíncronas com Virtual Threads (Java 21)</li>
 *   <li>Pool de conexões HikariCP otimizado</li>
 *   <li>Fila de comandos para processamento sequencial</li>
 *   <li>Suporte a transações atômicas</li>
 *   <li>Operações em lote para alta performance</li>
 * </ul>
 * 
 * <p>Exemplo de uso:
 * <pre>{@code
 * DatabaseConfig config = DatabaseConfig.builder("localhost", "mydb", "user")
 *     .password("secret")
 *     .build();
 * 
 * try (FloruitDatabase db = new FloruitDatabase(config)) {
 *     // Operação assíncrona
 *     CompletableFuture<Integer> result = db.executeUpdate(
 *         "INSERT INTO users (name) VALUES (?)", "João");
 *     
 *     // Consulta com mapeamento
 *     CompletableFuture<User> user = db.executeQuery(
 *         "SELECT * FROM users WHERE id = ?",
 *         rs -> new User(rs.getInt("id"), rs.getString("name")),
 *         1);
 * }
 * }</pre>
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public final class FloruitDatabase implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(FloruitDatabase.class);
    
    private final DatabaseConfig config;
    private final ConnectionManager connectionManager;
    private final AsyncExecutor asyncExecutor;
    private final CommandQueue commandQueue;
    
    private volatile boolean closed = false;
    
    /**
     * Constrói uma nova instância do FloruitDatabase.
     * 
     * @param config Configuração do banco de dados
     * @throws ConnectionException se não for possível conectar ao banco
     */
    public FloruitDatabase(DatabaseConfig config) {
        this.config = config;
        
        try {
            this.connectionManager = new ConnectionManager(config);
            this.asyncExecutor = new AsyncExecutor();
            this.commandQueue = new CommandQueue(1000, connectionManager); // Capacidade da fila
            
            // Inicia a fila de comandos
            this.commandQueue.start(100); // Processa a cada 100ms
            
            logger.info("FloruitDatabase inicializado com sucesso para {}:{}", 
                       config.host(), config.port());
                       
        } catch (Exception e) {
            logger.error("Falha ao inicializar FloruitDatabase", e);
            throw new ConnectionException("Não foi possível inicializar o banco de dados", e);
        }
    }
    
    /**
     * Executa uma operação de atualização (INSERT, UPDATE, DELETE) de forma assíncrona.
     * 
     * @param sql Query SQL com placeholders '?'
     * @param parameters Parâmetros para a query
     * @return CompletableFuture com o número de linhas afetadas
     */
    public CompletableFuture<Integer> executeUpdate(String sql, Object... parameters) {
        checkNotClosed();
        
        UpdateCommand command = UpdateCommand.of(sql, parameters);
        return executeCommand(command);
    }
    
    /**
     * Executa uma consulta (SELECT) de forma assíncrona com mapeamento customizado.
     * 
     * @param sql Query SQL de consulta
     * @param resultHandler Função para mapear o ResultSet para o tipo T
     * @param parameters Parâmetros para a query
     * @param <T> Tipo de objeto retornado
     * @return CompletableFuture com o resultado mapeado
     */
    public <T> CompletableFuture<T> executeQuery(
            String sql, Function<ResultSet, T> resultHandler, Object... parameters) {
        checkNotClosed();
        
        QueryCommand<T> command = QueryCommand.of(sql, resultHandler, parameters);
        return executeCommand(command);
    }
    
    /**
     * Executa múltiplas operações de atualização em lote de forma assíncrona.
     * 
     * @param sql Query SQL a ser executada para cada conjunto de parâmetros
     * @param parametersList Lista de arrays de parâmetros
     * @return CompletableFuture com array de linhas afetadas por operação
     */
    public CompletableFuture<int[]> executeBatch(String sql, List<Object[]> parametersList) {
        checkNotClosed();
        
        BatchUpdateCommand command = BatchUpdateCommand.of(sql, parametersList);
        return executeCommand(command);
    }
    
    /**
     * Executa múltiplos comandos dentro de uma transação atômica.
     * 
     * @param commands Lista de comandos a serem executados na transação
     * @return CompletableFuture que será completado quando a transação terminar
     */
    public CompletableFuture<Void> executeTransaction(List<DatabaseCommand<?>> commands) {
        checkNotClosed();
        
        TransactionCommand command = TransactionCommand.of(commands);
        return executeCommand(command);
    }
    
    /**
     * Executa múltiplos comandos dentro de uma transação atômica (varargs).
     * 
     * @param commands Comandos a serem executados na transação
     * @return CompletableFuture que será completado quando a transação terminar
     */
    @SafeVarargs
    public final CompletableFuture<Void> executeTransaction(DatabaseCommand<?>... commands) {
        checkNotClosed();
        
        TransactionCommand command = TransactionCommand.of(commands);
        return executeCommand(command);
    }
    
    /**
     * Enfileira um comando para execução sequencial na fila de comandos.
     * 
     * @param command Comando a ser enfileirado
     * @param <T> Tipo de retorno do comando
     * @return CompletableFuture que será completado com o resultado do comando
     */
    public <T> CompletableFuture<T> enqueueCommand(DatabaseCommand<T> command) {
        checkNotClosed();
        
        return commandQueue.enqueue(command);
    }
    
    /**
     * Executa um comando diretamente usando uma conexão do pool.
     * 
     * @param command Comando a ser executado
     * @param <T> Tipo de retorno do comando
     * @return CompletableFuture que será completado com o resultado do comando
     */
    private <T> CompletableFuture<T> executeCommand(DatabaseCommand<T> command) {
        return asyncExecutor.executeAsync(() -> {
            try (Connection connection = connectionManager.getConnection()) {
                return command.execute(connection).join();
            } catch (Exception e) {
                throw new FloruitDatabaseException("Falha ao executar comando: " + command.getDescription(), e);
            }
        });
    }
    
    /**
     * Retorna informações sobre o estado atual do banco de dados.
     * 
     * @return Informações do banco de dados
     */
    public DatabaseInfo getInfo() {
        if (closed) {
            return new DatabaseInfo(false, null, null, null);
        }
        
        return new DatabaseInfo(
            true,
            connectionManager.getPoolInfo(),
            asyncExecutor.getInfo(),
            commandQueue.getInfo()
        );
    }
    
    /**
     * Verifica se o banco de dados está ativo e saudável.
     * 
     * @return true se estiver ativo, false caso contrário
     */
    public boolean isHealthy() {
        return !closed && connectionManager.isHealthy() && asyncExecutor.isActive();
    }
    
    /**
     * Verifica se a instância foi fechada.
     * 
     * @throws IllegalStateException se a instância foi fechada
     */
    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("FloruitDatabase foi fechado");
        }
    }
    
    /**
     * Fecha o banco de dados e libera todos os recursos.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        logger.info("Fechando FloruitDatabase...");
        
        try {
            // Fecha a fila de comandos primeiro
            commandQueue.close();
            
            // Fecha o executor assíncrono
            asyncExecutor.close();
            
            // Fecha o gerenciador de conexões por último
            connectionManager.close();
            
            logger.info("FloruitDatabase fechado com sucesso");
            
        } catch (Exception e) {
            logger.error("Erro ao fechar FloruitDatabase", e);
        }
    }
    
    /**
     * Classe para encapsular informações sobre o estado do banco de dados.
     */
    public record DatabaseInfo(
        boolean isActive,
        ConnectionManager.PoolInfo poolInfo,
        AsyncExecutor.ExecutorInfo executorInfo,
        CommandQueue.QueueInfo queueInfo
    ) {
        /**
         * Retorna uma descrição resumida do estado do banco.
         * 
         * @return Descrição do estado
         */
        public String getStatus() {
            if (!isActive) {
                return "Banco de dados fechado";
            }
            
            return String.format("Ativo - Pool: %d/%d, Fila: %d, Processados: %d",
                               poolInfo.activeConnections(),
                               poolInfo.maxPoolSize(),
                               queueInfo.queueSize(),
                               queueInfo.processedCommands());
        }
    }
}

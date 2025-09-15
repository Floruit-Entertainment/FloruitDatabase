package com.hanielcota.floruitdatabase.core;

import com.hanielcota.floruitdatabase.config.DatabaseConfig;
import com.hanielcota.floruitdatabase.exception.ConnectionException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Gerenciador de conexões com banco de dados usando HikariCP.
 * 
 * <p>Esta classe encapsula toda a lógica de criação e gerenciamento do pool de
 * conexões HikariCP, fornecendo uma interface limpa para obtenção de conexões
 * e gerenciamento do ciclo de vida do pool.
 * 
 * <p>Implementa o padrão Singleton para garantir uma única instância do pool
 * por configuração de banco de dados.
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public final class ConnectionManager implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    
    private final DatabaseConfig config;
    private final HikariDataSource dataSource;
    private volatile boolean closed = false;
    
    /**
     * Constrói um novo gerenciador de conexões com a configuração especificada.
     * 
     * @param config Configuração do banco de dados
     * @throws ConnectionException se não for possível criar o pool de conexões
     */
    public ConnectionManager(DatabaseConfig config) {
        this.config = Objects.requireNonNull(config, "Configuração não pode ser nula");
        this.dataSource = createDataSource();
        
        // Testa a conexão inicial
        testConnection();
        
        logger.info("Pool de conexões criado com sucesso para {}:{}", 
                   config.host(), config.port());
    }
    
    /**
     * Obtém uma conexão do pool.
     * 
     * @return Conexão com o banco de dados
     * @throws ConnectionException se não for possível obter uma conexão
     */
    public Connection getConnection() {
        if (closed) {
            throw new ConnectionException("Gerenciador de conexões foi fechado");
        }
        
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new ConnectionException("Falha ao obter conexão do pool", e);
        }
    }
    
    /**
     * Verifica se o pool de conexões está ativo e saudável.
     * 
     * @return true se o pool estiver ativo, false caso contrário
     */
    public boolean isHealthy() {
        return !closed && dataSource != null && !dataSource.isClosed();
    }
    
    /**
     * Retorna informações sobre o estado atual do pool.
     * 
     * @return Informações do pool de conexões
     */
    public PoolInfo getPoolInfo() {
        if (closed || dataSource == null || dataSource.isClosed()) {
            return new PoolInfo(0, 0, 0, 0, 0);
        }
        
        return new PoolInfo(
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection(),
            dataSource.getHikariPoolMXBean().getTotalConnections()
        );
    }
    
    /**
     * Fecha o pool de conexões e libera todos os recursos.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Fechando pool de conexões...");
            dataSource.close();
            logger.info("Pool de conexões fechado com sucesso");
        }
    }
    
    /**
     * Cria e configura o HikariDataSource com base na configuração.
     * 
     * @return HikariDataSource configurado
     * @throws ConnectionException se não for possível criar o data source
     */
    private HikariDataSource createDataSource() {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            
            // Configurações básicas de conexão
            hikariConfig.setJdbcUrl(config.getJdbcUrl());
            hikariConfig.setUsername(config.username());
            hikariConfig.setPassword(config.password());
            
            // Configurações do pool
            hikariConfig.setMaximumPoolSize(config.maxPoolSize());
            hikariConfig.setMinimumIdle(config.minIdle());
            hikariConfig.setConnectionTimeout(config.connectionTimeout().toMillis());
            hikariConfig.setIdleTimeout(config.idleTimeout().toMillis());
            hikariConfig.setMaxLifetime(config.maxLifetime().toMillis());
            
            // Configurações de detecção de vazamento
            if (config.leakDetectionThreshold().toMillis() > 0) {
                hikariConfig.setLeakDetectionThreshold(config.leakDetectionThreshold().toMillis());
            }
            
            // Configurações de performance do MySQL
            hikariConfig.addDataSourceProperty("cachePrepStmts", config.cachePrepStmts());
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", config.prepStmtCacheSize());
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", config.prepStmtCacheSqlLimit());
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
            hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
            hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
            hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
            hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
            
            // Configurações de conexão MySQL
            hikariConfig.addDataSourceProperty("useSSL", "false");
            hikariConfig.addDataSourceProperty("allowPublicKeyRetrieval", "true");
            hikariConfig.addDataSourceProperty("serverTimezone", "UTC");
            hikariConfig.addDataSourceProperty("characterEncoding", "utf8");
            hikariConfig.addDataSourceProperty("useUnicode", "true");
            
            // Nome do pool para identificação
            hikariConfig.setPoolName("FloruitDB-Pool-" + config.database());
            
            return new HikariDataSource(hikariConfig);
            
        } catch (Exception e) {
            throw new ConnectionException("Falha ao criar pool de conexões HikariCP", e);
        }
    }
    
    /**
     * Testa a conectividade com o banco de dados.
     * 
     * @throws ConnectionException se não for possível conectar
     */
    private void testConnection() {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) {
                throw new ConnectionException("Conexão de teste falhou - banco não está acessível");
            }
            logger.debug("Teste de conexão bem-sucedido");
        } catch (SQLException e) {
            throw new ConnectionException("Falha no teste de conexão inicial", e);
        }
    }
    
    /**
     * Classe para encapsular informações sobre o estado do pool.
     */
    public record PoolInfo(
        int totalConnections,
        int activeConnections,
        int idleConnections,
        int threadsAwaitingConnection,
        int maxPoolSize
    ) {
        /**
         * Calcula a taxa de utilização do pool.
         * 
         * @return Taxa de utilização (0.0 a 1.0)
         */
        public double getUtilizationRate() {
            return maxPoolSize > 0 ? (double) activeConnections / maxPoolSize : 0.0;
        }
        
        /**
         * Verifica se o pool está próximo da capacidade máxima.
         * 
         * @return true se a utilização for maior que 80%
         */
        public boolean isNearCapacity() {
            return getUtilizationRate() > 0.8;
        }
    }
}

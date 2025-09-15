package com.hanielcota.floruitdatabase.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Builder para construção fluente de configurações de banco de dados.
 * 
 * <p>Implementa o padrão Builder para permitir configuração flexível e legível
 * dos parâmetros de conexão com o banco de dados MySQL.
 * 
 * <p>Exemplo de uso:
 * <pre>{@code
 * DatabaseConfig config = DatabaseConfig.builder("localhost", "mydb", "user")
 *     .password("secret")
 *     .port(3306)
 *     .maxPoolSize(20)
 *     .connectionTimeout(Duration.ofSeconds(30))
 *     .build();
 * }</pre>
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public final class DatabaseConfigBuilder {
    
    // Parâmetros obrigatórios
    private final String host;
    private final String database;
    private final String username;
    
    // Parâmetros opcionais com valores padrão
    private int port = 3306;
    private String password = "";
    private int maxPoolSize = 10;
    private int minIdle = 5;
    private Duration connectionTimeout = Duration.ofSeconds(30);
    private Duration idleTimeout = Duration.ofMinutes(10);
    private Duration maxLifetime = Duration.ofMinutes(30);
    private Duration leakDetectionThreshold = Duration.ofSeconds(0); // Desabilitado por padrão
    private boolean cachePrepStmts = true;
    private int prepStmtCacheSize = 250;
    private int prepStmtCacheSqlLimit = 2048;
    
    /**
     * Constrói um novo builder com os parâmetros obrigatórios.
     * 
     * @param host Endereço do servidor de banco de dados
     * @param database Nome do banco de dados
     * @param username Nome de usuário para autenticação
     * @throws NullPointerException se algum parâmetro obrigatório for nulo
     */
    public DatabaseConfigBuilder(String host, String database, String username) {
        this.host = Objects.requireNonNull(host, "Host não pode ser nulo");
        this.database = Objects.requireNonNull(database, "Database não pode ser nulo");
        this.username = Objects.requireNonNull(username, "Username não pode ser nulo");
    }
    
    /**
     * Define a porta de conexão.
     * 
     * @param port Porta do servidor (padrão: 3306)
     * @return Este builder para encadeamento
     */
    public DatabaseConfigBuilder port(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Porta deve estar entre 1 e 65535");
        }
        this.port = port;
        return this;
    }
    
    /**
     * Define a senha de autenticação.
     * 
     * @param password Senha (padrão: string vazia)
     * @return Este builder para encadeamento
     */
    public DatabaseConfigBuilder password(String password) {
        this.password = password != null ? password : "";
        return this;
    }
    
    /**
     * Define o tamanho máximo do pool de conexões.
     * 
     * @param maxPoolSize Tamanho máximo (padrão: 10)
     * @return Este builder para encadeamento
     */
    public DatabaseConfigBuilder maxPoolSize(int maxPoolSize) {
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("Tamanho máximo do pool deve ser positivo");
        }
        this.maxPoolSize = maxPoolSize;
        return this;
    }
    
    /**
     * Define o número mínimo de conexões idle.
     * 
     * @param minIdle Número mínimo de conexões idle (padrão: 5)
     * @return Este builder para encadeamento
     */
    public DatabaseConfigBuilder minIdle(int minIdle) {
        if (minIdle < 0) {
            throw new IllegalArgumentException("Min idle não pode ser negativo");
        }
        this.minIdle = minIdle;
        return this;
    }
    
    /**
     * Define o timeout para obter conexão do pool.
     * 
     * @param connectionTimeout Timeout (padrão: 30 segundos)
     * @return Este builder para encadeamento
     */
    public DatabaseConfigBuilder connectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = Objects.requireNonNull(connectionTimeout, "Connection timeout não pode ser nulo");
        return this;
    }
    
    /**
     * Define o timeout para conexões idle.
     * 
     * @param idleTimeout Timeout (padrão: 10 minutos)
     * @return Este builder para encadeamento
     */
    public DatabaseConfigBuilder idleTimeout(Duration idleTimeout) {
        this.idleTimeout = Objects.requireNonNull(idleTimeout, "Idle timeout não pode ser nulo");
        return this;
    }
    
    /**
     * Define o tempo máximo de vida de uma conexão.
     * 
     * @param maxLifetime Tempo máximo de vida (padrão: 30 minutos)
     * @return Este builder para encadeamento
     */
    public DatabaseConfigBuilder maxLifetime(Duration maxLifetime) {
        this.maxLifetime = Objects.requireNonNull(maxLifetime, "Max lifetime não pode ser nulo");
        return this;
    }
    
    /**
     * Define o threshold para detecção de vazamento de conexões.
     * 
     * @param leakDetectionThreshold Threshold (padrão: 0 - desabilitado)
     * @return Este builder para encadeamento
     */
    public DatabaseConfigBuilder leakDetectionThreshold(Duration leakDetectionThreshold) {
        this.leakDetectionThreshold = Objects.requireNonNull(leakDetectionThreshold, "Leak detection threshold não pode ser nulo");
        return this;
    }
    
    /**
     * Habilita ou desabilita o cache de prepared statements.
     * 
     * @param cachePrepStmts true para habilitar cache (padrão: true)
     * @return Este builder para encadeamento
     */
    public DatabaseConfigBuilder cachePrepStmts(boolean cachePrepStmts) {
        this.cachePrepStmts = cachePrepStmts;
        return this;
    }
    
    /**
     * Define o tamanho do cache de prepared statements.
     * 
     * @param prepStmtCacheSize Tamanho do cache (padrão: 250)
     * @return Este builder para encadeamento
     */
    public DatabaseConfigBuilder prepStmtCacheSize(int prepStmtCacheSize) {
        if (prepStmtCacheSize <= 0) {
            throw new IllegalArgumentException("Tamanho do cache deve ser positivo");
        }
        this.prepStmtCacheSize = prepStmtCacheSize;
        return this;
    }
    
    /**
     * Define o limite de tamanho SQL no cache.
     * 
     * @param prepStmtCacheSqlLimit Limite em caracteres (padrão: 2048)
     * @return Este builder para encadeamento
     */
    public DatabaseConfigBuilder prepStmtCacheSqlLimit(int prepStmtCacheSqlLimit) {
        if (prepStmtCacheSqlLimit <= 0) {
            throw new IllegalArgumentException("Limite do cache SQL deve ser positivo");
        }
        this.prepStmtCacheSqlLimit = prepStmtCacheSqlLimit;
        return this;
    }
    
    /**
     * Constrói a configuração final validando todos os parâmetros.
     * 
     * @return Configuração imutável do banco de dados
     * @throws IllegalArgumentException se minIdle for maior que maxPoolSize
     */
    public DatabaseConfig build() {
        if (minIdle > maxPoolSize) {
            throw new IllegalArgumentException("Min idle não pode ser maior que maxPoolSize");
        }
        
        return new DatabaseConfig(
            host, port, database, username, password,
            maxPoolSize, minIdle, connectionTimeout, idleTimeout, maxLifetime,
            leakDetectionThreshold, cachePrepStmts, prepStmtCacheSize, prepStmtCacheSqlLimit
        );
    }
}

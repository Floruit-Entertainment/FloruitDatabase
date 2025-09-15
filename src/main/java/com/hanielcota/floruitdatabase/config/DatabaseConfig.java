package com.hanielcota.floruitdatabase.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuração imutável para conexão com banco de dados MySQL.
 * 
 * <p>Esta classe representa todas as configurações necessárias para estabelecer
 * uma conexão com o banco de dados, incluindo parâmetros de performance e
 * configurações do pool de conexões HikariCP.
 * 
 * @param host Endereço do servidor de banco de dados
 * @param port Porta de conexão (padrão: 3306)
 * @param database Nome do banco de dados
 * @param username Nome de usuário para autenticação
 * @param password Senha para autenticação
 * @param maxPoolSize Tamanho máximo do pool de conexões
 * @param minIdle Tamanho mínimo de conexões idle
 * @param connectionTimeout Timeout para obter conexão do pool
 * @param idleTimeout Timeout para conexões idle
 * @param maxLifetime Tempo máximo de vida de uma conexão
 * @param leakDetectionThreshold Threshold para detecção de vazamento de conexões
 * @param cachePrepStmts Habilita cache de prepared statements
 * @param prepStmtCacheSize Tamanho do cache de prepared statements
 * @param prepStmtCacheSqlLimit Limite de tamanho SQL no cache
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public record DatabaseConfig(
    String host,
    int port,
    String database,
    String username,
    String password,
    int maxPoolSize,
    int minIdle,
    Duration connectionTimeout,
    Duration idleTimeout,
    Duration maxLifetime,
    Duration leakDetectionThreshold,
    boolean cachePrepStmts,
    int prepStmtCacheSize,
    int prepStmtCacheSqlLimit
) {
    
    /**
     * Construtor principal que valida os parâmetros obrigatórios.
     */
    public DatabaseConfig {
        Objects.requireNonNull(host, "Host não pode ser nulo");
        Objects.requireNonNull(database, "Nome do banco não pode ser nulo");
        Objects.requireNonNull(username, "Username não pode ser nulo");
        Objects.requireNonNull(password, "Password não pode ser nulo");
        
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Porta deve estar entre 1 e 65535");
        }
        
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("Tamanho máximo do pool deve ser positivo");
        }
        
        if (minIdle < 0 || minIdle > maxPoolSize) {
            throw new IllegalArgumentException("Min idle deve estar entre 0 e maxPoolSize");
        }
    }
    
    /**
     * Retorna a URL JDBC completa para conexão com MySQL.
     * 
     * @return URL JDBC formatada
     */
    public String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", 
                           host, port, database);
    }
    
    /**
     * Cria um builder para construção fluente da configuração.
     * 
     * @param host Endereço do servidor
     * @param database Nome do banco de dados
     * @param username Nome de usuário
     * @return Builder para configuração
     */
    public static DatabaseConfigBuilder builder(String host, String database, String username) {
        return new DatabaseConfigBuilder(host, database, username);
    }
    
    /**
     * Cria uma configuração com valores padrão otimizados.
     * 
     * @param host Endereço do servidor
     * @param database Nome do banco de dados
     * @param username Nome de usuário
     * @param password Senha
     * @return Configuração com valores padrão
     */
    public static DatabaseConfig defaults(String host, String database, String username, String password) {
        return builder(host, database, username)
            .password(password)
            .build();
    }
}

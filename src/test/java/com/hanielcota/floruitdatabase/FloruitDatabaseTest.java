package com.hanielcota.floruitdatabase;

import com.hanielcota.floruitdatabase.config.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes básicos para o FloruitDatabase.
 * 
 * <p>Estes testes verificam a funcionalidade básica da biblioteca,
 * incluindo configuração, inicialização e operações simples.
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public class FloruitDatabaseTest {
    
    private DatabaseConfig config;
    
    @BeforeEach
    void setUp() {
        // Configuração de teste (requer banco MySQL em execução)
        config = DatabaseConfig.builder("localhost", "test_db", "root")
            .password("password")
            .port(3306)
            .maxPoolSize(5)
            .minIdle(2)
            .build();
    }
    
    @Test
    void testDatabaseConfigCreation() {
        assertNotNull(config);
        assertEquals("localhost", config.host());
        assertEquals(3306, config.port());
        assertEquals("test_db", config.database());
        assertEquals("root", config.username());
        assertEquals("password", config.password());
        assertEquals(5, config.maxPoolSize());
        assertEquals(2, config.minIdle());
    }
    
    @Test
    void testDatabaseConfigBuilder() {
        DatabaseConfig customConfig = DatabaseConfig.builder("example.com", "custom_db", "user")
            .password("secret")
            .port(5432)
            .maxPoolSize(15)
            .minIdle(3)
            .build();
        
        assertEquals("example.com", customConfig.host());
        assertEquals(5432, customConfig.port());
        assertEquals("custom_db", customConfig.database());
        assertEquals("user", customConfig.username());
        assertEquals("secret", customConfig.password());
        assertEquals(15, customConfig.maxPoolSize());
        assertEquals(3, customConfig.minIdle());
    }
    
    @Test
    void testDatabaseConfigValidation() {
        // Teste de validação de porta inválida
        assertThrows(IllegalArgumentException.class, () -> {
            DatabaseConfig.builder("localhost", "test", "user")
                .port(0)
                .build();
        });
        
        // Teste de validação de maxPoolSize inválido
        assertThrows(IllegalArgumentException.class, () -> {
            DatabaseConfig.builder("localhost", "test", "user")
                .maxPoolSize(-1)
                .build();
        });
        
        // Teste de validação de minIdle maior que maxPoolSize
        assertThrows(IllegalArgumentException.class, () -> {
            DatabaseConfig.builder("localhost", "test", "user")
                .maxPoolSize(5)
                .minIdle(10)
                .build();
        });
    }
    
    @Test
    void testJdbcUrlGeneration() {
        String jdbcUrl = config.getJdbcUrl();
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:mysql://"));
        assertTrue(jdbcUrl.contains("localhost:3306/test_db"));
        assertTrue(jdbcUrl.contains("useSSL=false"));
        assertTrue(jdbcUrl.contains("allowPublicKeyRetrieval=true"));
        assertTrue(jdbcUrl.contains("serverTimezone=UTC"));
    }
    
    @Test
    void testDefaultConfiguration() {
        DatabaseConfig defaultConfig = DatabaseConfig.defaults("localhost", "test", "user", "pass");
        
        assertNotNull(defaultConfig);
        assertEquals("localhost", defaultConfig.host());
        assertEquals(3306, defaultConfig.port()); // Porta padrão
        assertEquals("test", defaultConfig.database());
        assertEquals("user", defaultConfig.username());
        assertEquals("pass", defaultConfig.password());
        assertEquals(10, defaultConfig.maxPoolSize()); // Valor padrão
        assertEquals(5, defaultConfig.minIdle()); // Valor padrão
    }
    
    // Nota: Testes de integração com banco real seriam executados apenas em ambiente de CI/CD
    // com um banco MySQL configurado especificamente para testes
}

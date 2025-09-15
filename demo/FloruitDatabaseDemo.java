package demo;

import com.hanielcota.floruitdatabase.FloruitDatabase;
import com.hanielcota.floruitdatabase.command.UpdateCommand;
import com.hanielcota.floruitdatabase.config.DatabaseConfig;
import com.hanielcota.floruitdatabase.util.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Demonstração de uso do FloruitDatabase.
 * 
 * <p>Este exemplo mostra como usar a biblioteca FloruitDatabase para
 * realizar operações assíncronas com banco de dados MySQL, incluindo
 * operações básicas, transações e processamento em lote.
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public class FloruitDatabaseDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(FloruitDatabaseDemo.class);
    
    public static void main(String[] args) {
        // Configuração do banco de dados
        DatabaseConfig config = DatabaseConfig.builder("localhost", "floruit_demo", "root")
            .password("password")
            .port(3306)
            .maxPoolSize(10)
            .minIdle(5)
            .build();
        
        try (FloruitDatabase db = new FloruitDatabase(config)) {
            
            logger.info("=== Demonstração FloruitDatabase ===");
            
            // Demonstração de operações básicas
            demonstrateBasicOperations(db);
            
            // Demonstração de operações em lote
            demonstrateBatchOperations(db);
            
            // Demonstração de transações
            demonstrateTransactions(db);
            
            // Demonstração de consultas complexas
            demonstrateComplexQueries(db);
            
            // Informações do sistema
            showSystemInfo(db);
            
        } catch (Exception e) {
            logger.error("Erro na demonstração", e);
        }
    }
    
    /**
     * Demonstra operações básicas de CRUD.
     */
    private static void demonstrateBasicOperations(FloruitDatabase db) {
        logger.info("\n--- Operações Básicas ---");
        
        try {
            // Criar tabela
            db.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """).join();
            
            logger.info("Tabela 'users' criada com sucesso");
            
            // Inserir usuário
            CompletableFuture<Integer> insertResult = db.executeUpdate(
                "INSERT INTO users (name, email) VALUES (?, ?)",
                "João Silva", "joao@example.com"
            );
            
            int rowsAffected = insertResult.join();
            logger.info("Usuário inserido: {} linhas afetadas", rowsAffected);
            
            // Consultar usuário
            CompletableFuture<User> userResult = db.executeQuery(
                "SELECT * FROM users WHERE email = ?",
                FloruitDatabaseDemo::mapUser,
                "joao@example.com"
            );
            
            User user = userResult.join();
            logger.info("Usuário encontrado: {}", user);
            
            // Atualizar usuário
            CompletableFuture<Integer> updateResult = db.executeUpdate(
                "UPDATE users SET name = ? WHERE email = ?",
                "João Santos", "joao@example.com"
            );
            
            int updatedRows = updateResult.join();
            logger.info("Usuário atualizado: {} linhas afetadas", updatedRows);
            
        } catch (Exception e) {
            logger.error("Erro nas operações básicas", e);
        }
    }
    
    /**
     * Demonstra operações em lote.
     */
    private static void demonstrateBatchOperations(FloruitDatabase db) {
        logger.info("\n--- Operações em Lote ---");
        
        try {
            // Preparar dados para inserção em lote
            List<Object[]> batchData = List.of(
                new Object[]{"Maria Silva", "maria@example.com"},
                new Object[]{"Pedro Santos", "pedro@example.com"},
                new Object[]{"Ana Costa", "ana@example.com"},
                new Object[]{"Carlos Lima", "carlos@example.com"}
            );
            
            // Executar inserção em lote
            CompletableFuture<int[]> batchResult = db.executeBatch(
                "INSERT INTO users (name, email) VALUES (?, ?)",
                batchData
            );
            
            int[] results = batchResult.join();
            int totalInserted = 0;
            for (int result : results) {
                totalInserted += result;
            }
            
            logger.info("Inserção em lote concluída: {} usuários inseridos", totalInserted);
            
        } catch (Exception e) {
            logger.error("Erro nas operações em lote", e);
        }
    }
    
    /**
     * Demonstra transações atômicas.
     */
    private static void demonstrateTransactions(FloruitDatabase db) {
        logger.info("\n--- Transações ---");
        
        try {
            // Criar tabela de pedidos
            db.executeUpdate("""
                CREATE TABLE IF NOT EXISTS orders (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    product VARCHAR(100) NOT NULL,
                    quantity INT NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """).join();
            
            // Transação: criar pedido e atualizar estoque (simulado)
            CompletableFuture<Void> transactionResult = db.executeTransaction(
                // Inserir pedido
                UpdateCommand.of(
                    "INSERT INTO orders (user_id, product, quantity, price) VALUES (?, ?, ?, ?)",
                    1, "Produto A", 2, 29.99
                ),
                
                // Simular atualização de estoque
                UpdateCommand.of(
                    "UPDATE users SET name = CONCAT(name, ' - Cliente Ativo') WHERE id = ?",
                    1
                )
            );
            
            transactionResult.join();
            logger.info("Transação executada com sucesso");
            
        } catch (Exception e) {
            logger.error("Erro na transação", e);
        }
    }
    
    /**
     * Demonstra consultas complexas.
     */
    private static void demonstrateComplexQueries(FloruitDatabase db) {
        logger.info("\n--- Consultas Complexas ---");
        
        try {
            // Consulta com agregação
            CompletableFuture<Integer> countResult = db.executeQuery(
                "SELECT COUNT(*) as total FROM users",
                rs -> rs.getInt("total")
            );
            
            int totalUsers = countResult.join();
            logger.info("Total de usuários: {}", totalUsers);
            
            // Consulta com JOIN (simulada)
            CompletableFuture<List<User>> usersResult = db.executeQuery(
                "SELECT u.*, COUNT(o.id) as order_count FROM users u LEFT JOIN orders o ON u.id = o.user_id GROUP BY u.id",
                FloruitDatabaseDemo::mapUserWithOrderCount
            );
            
            List<User> users = usersResult.join();
            logger.info("Usuários com contagem de pedidos: {}", users.size());
            
            // Consulta paginada
            CompletableFuture<List<User>> pagedResult = db.executeQuery(
                "SELECT * FROM users ORDER BY created_at DESC LIMIT ? OFFSET ?",
                FloruitDatabaseDemo::mapUser,
                5, 0
            );
            
            List<User> pagedUsers = pagedResult.join();
            logger.info("Usuários (primeira página): {}", pagedUsers.size());
            
        } catch (Exception e) {
            logger.error("Erro nas consultas complexas", e);
        }
    }
    
    /**
     * Mostra informações do sistema.
     */
    private static void showSystemInfo(FloruitDatabase db) {
        logger.info("\n--- Informações do Sistema ---");
        
        var info = db.getInfo();
        logger.info("Status: {}", info.getStatus());
        logger.info("Pool de conexões: {} ativas de {}", 
                   info.poolInfo().activeConnections(), 
                   info.poolInfo().maxPoolSize());
        logger.info("Fila de comandos: {} pendentes, {} processados", 
                   info.queueInfo().queueSize(), 
                   info.queueInfo().processedCommands());
        logger.info("Taxa de sucesso: {:.2f}%", 
                   info.queueInfo().getSuccessRate() * 100);
    }
    
    /**
     * Mapeia um ResultSet para um objeto User.
     */
    private static User mapUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
    
    /**
     * Mapeia um ResultSet para um objeto User com contagem de pedidos.
     */
    private static List<User> mapUserWithOrderCount(ResultSet rs) throws SQLException {
        return ResultSetMapper.mapAll(rs, resultSet -> {
            try {
                return new User(
                    resultSet.getInt("id"),
                    resultSet.getString("name"),
                    resultSet.getString("email"),
                    resultSet.getTimestamp("created_at").toLocalDateTime()
                );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Classe de exemplo para representar um usuário.
     */
    public record User(
        int id,
        String name,
        String email,
        java.time.LocalDateTime createdAt
    ) {
        @Override
        public String toString() {
            return String.format("User{id=%d, name='%s', email='%s', createdAt=%s}", 
                               id, name, email, createdAt);
        }
    }
}

package com.hanielcota.floruitdatabase.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Comando para execução de múltiplas operações dentro de uma transação.
 * 
 * <p>Este comando encapsula a execução de uma sequência de comandos dentro
 * de uma transação atômica, garantindo que todas as operações sejam executadas
 * com sucesso ou todas sejam revertidas.
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public final class TransactionCommand implements DatabaseCommand<Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionCommand.class);
    
    private final List<DatabaseCommand<?>> commands;
    
    /**
     * Constrói um novo comando de transação.
     * 
     * @param commands Lista de comandos a serem executados na transação
     */
    private TransactionCommand(List<DatabaseCommand<?>> commands) {
        this.commands = Objects.requireNonNull(commands, "Lista de comandos não pode ser nula");
    }
    
    /**
     * Cria um novo comando de transação.
     * 
     * @param commands Lista de comandos a serem executados na transação
     * @return Novo comando de transação
     */
    public static TransactionCommand of(List<DatabaseCommand<?>> commands) {
        return new TransactionCommand(commands);
    }
    
    /**
     * Cria um novo comando de transação com comandos variádicos.
     * 
     * @param commands Comandos a serem executados na transação
     * @return Novo comando de transação
     */
    @SafeVarargs
    public static TransactionCommand of(DatabaseCommand<?>... commands) {
        return new TransactionCommand(List.of(commands));
    }
    
    @Override
    public CompletableFuture<Void> execute(Connection connection) {
        return CompletableFuture.runAsync(() -> {
            boolean originalAutoCommit = true;
            
            try {
                // Salva o estado original do auto-commit
                originalAutoCommit = connection.getAutoCommit();
                
                // Inicia a transação
                connection.setAutoCommit(false);
                logger.debug("Transação iniciada com {} comandos", commands.size());
                
                // Executa todos os comandos sequencialmente
                for (int i = 0; i < commands.size(); i++) {
                    DatabaseCommand<?> command = commands.get(i);
                    
                    try {
                        logger.debug("Executando comando {}/{}: {}", i + 1, commands.size(), command.getDescription());
                        
                        // Executa o comando e aguarda a conclusão
                        command.execute(connection).join();
                        
                    } catch (Exception e) {
                        logger.error("Comando {}/{} falhou: {}", i + 1, commands.size(), command.getDescription(), e);
                        throw new RuntimeException("Comando falhou na transação", e);
                    }
                }
                
                // Commit da transação
                connection.commit();
                logger.debug("Transação commitada com sucesso");
                
            } catch (Exception e) {
                try {
                    // Rollback da transação
                    connection.rollback();
                    logger.debug("Transação revertida devido a erro");
                } catch (SQLException rollbackException) {
                    logger.error("Erro ao fazer rollback da transação", rollbackException);
                }
                
                throw new RuntimeException("Transação falhou", e);
                
            } finally {
                try {
                    // Restaura o estado original do auto-commit
                    connection.setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                    logger.error("Erro ao restaurar auto-commit", e);
                }
            }
        });
    }
    
    @Override
    public String getDescription() {
        return String.format("TransactionCommand[%d comandos]", commands.size());
    }
    
    @Override
    public boolean isReadOnly() {
        // Uma transação é considerada de escrita se pelo menos um comando for de escrita
        return commands.stream().allMatch(DatabaseCommand::isReadOnly);
    }
    
    @Override
    public boolean requiresTransaction() {
        return true;
    }
    
    /**
     * Retorna o número de comandos na transação.
     * 
     * @return Número de comandos
     */
    public int getCommandCount() {
        return commands.size();
    }
    
    /**
     * Retorna a lista de comandos desta transação.
     * 
     * @return Lista de comandos
     */
    public List<DatabaseCommand<?>> getCommands() {
        return List.copyOf(commands);
    }
}

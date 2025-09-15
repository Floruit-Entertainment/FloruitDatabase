package com.hanielcota.floruitdatabase.command;

import com.hanielcota.floruitdatabase.exception.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Comando para execução de operações em lote (batch).
 * 
 * <p>Este comando encapsula a execução de múltiplas operações de atualização
 * em um único lote, proporcionando melhor performance para operações em massa.
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public final class BatchUpdateCommand implements DatabaseCommand<int[]> {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchUpdateCommand.class);
    
    private final String sql;
    private final List<Object[]> parametersList;
    
    /**
     * Constrói um novo comando de atualização em lote.
     * 
     * @param sql Query SQL a ser executada para cada conjunto de parâmetros
     * @param parametersList Lista de arrays de parâmetros
     */
    private BatchUpdateCommand(String sql, List<Object[]> parametersList) {
        this.sql = Objects.requireNonNull(sql, "SQL não pode ser nulo");
        this.parametersList = Objects.requireNonNull(parametersList, "Lista de parâmetros não pode ser nula");
    }
    
    /**
     * Cria um novo comando de atualização em lote.
     * 
     * @param sql Query SQL a ser executada para cada conjunto de parâmetros
     * @param parametersList Lista de arrays de parâmetros
     * @return Novo comando de atualização em lote
     */
    public static BatchUpdateCommand of(String sql, List<Object[]> parametersList) {
        return new BatchUpdateCommand(sql, parametersList);
    }
    
    @Override
    public CompletableFuture<int[]> execute(Connection connection) {
        return CompletableFuture.supplyAsync(() -> {
            if (parametersList.isEmpty()) {
                logger.debug("Lista de parâmetros vazia, retornando array vazio");
                return new int[0];
            }
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                
                // Adiciona cada conjunto de parâmetros ao batch
                for (Object[] parameters : parametersList) {
                    setParameters(statement, parameters);
                    statement.addBatch();
                }
                
                logger.debug("Executando batch update: {} com {} operações", sql, parametersList.size());
                
                int[] results = statement.executeBatch();
                
                int totalAffected = 0;
                for (int result : results) {
                    totalAffected += result;
                }
                
                logger.debug("Batch update executado com sucesso - {} linhas afetadas no total", totalAffected);
                return results;
                
            } catch (SQLException e) {
                logger.error("Erro ao executar batch update: {}", sql, e);
                throw new QueryException("Falha ao executar atualização em lote", sql, e);
            }
        });
    }
    
    @Override
    public String getDescription() {
        return String.format("BatchUpdateCommand[%s, %d operações]", 
                           sql.length() > 30 ? sql.substring(0, 30) + "..." : sql, 
                           parametersList.size());
    }
    
    @Override
    public boolean isReadOnly() {
        return false;
    }
    
    @Override
    public boolean requiresTransaction() {
        return true; // Operações em lote devem ser executadas em transação
    }
    
    /**
     * Define os parâmetros no PreparedStatement.
     * 
     * @param statement PreparedStatement a ser configurado
     * @param parameters Parâmetros a serem definidos
     * @throws SQLException se ocorrer erro ao definir parâmetros
     */
    private void setParameters(PreparedStatement statement, Object[] parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }
    
    /**
     * Retorna a query SQL deste comando.
     * 
     * @return Query SQL
     */
    public String getSql() {
        return sql;
    }
    
    /**
     * Retorna o número de operações no lote.
     * 
     * @return Número de operações
     */
    public int getBatchSize() {
        return parametersList.size();
    }
    
    /**
     * Retorna a lista de parâmetros deste comando.
     * 
     * @return Lista de arrays de parâmetros
     */
    public List<Object[]> getParametersList() {
        return List.copyOf(parametersList);
    }
}

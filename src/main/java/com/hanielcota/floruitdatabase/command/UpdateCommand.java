package com.hanielcota.floruitdatabase.command;

import com.hanielcota.floruitdatabase.exception.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Comando para execução de operações de atualização (INSERT, UPDATE, DELETE).
 * 
 * <p>Este comando encapsula a execução de operações que modificam dados no
 * banco de dados, retornando o número de linhas afetadas.
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public final class UpdateCommand implements DatabaseCommand<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateCommand.class);
    
    private final String sql;
    private final Object[] parameters;
    
    /**
     * Constrói um novo comando de atualização.
     * 
     * @param sql Query SQL a ser executada
     * @param parameters Parâmetros da query
     */
    private UpdateCommand(String sql, Object... parameters) {
        this.sql = Objects.requireNonNull(sql, "SQL não pode ser nulo");
        this.parameters = parameters != null ? parameters : new Object[0];
    }
    
    /**
     * Cria um novo comando de atualização.
     * 
     * @param sql Query SQL a ser executada
     * @param parameters Parâmetros da query
     * @return Novo comando de atualização
     */
    public static UpdateCommand of(String sql, Object... parameters) {
        return new UpdateCommand(sql, parameters);
    }
    
    @Override
    public CompletableFuture<Integer> execute(Connection connection) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                
                // Define os parâmetros
                setParameters(statement, parameters);
                
                logger.debug("Executando update: {}", sql);
                
                int rowsAffected = statement.executeUpdate();
                
                logger.debug("Update executado com sucesso - {} linhas afetadas", rowsAffected);
                return rowsAffected;
                
            } catch (SQLException e) {
                logger.error("Erro ao executar update: {}", sql, e);
                throw new QueryException("Falha ao executar atualização", sql, e);
            }
        });
    }
    
    @Override
    public String getDescription() {
        return String.format("UpdateCommand[%s]", sql.length() > 50 ? sql.substring(0, 50) + "..." : sql);
    }
    
    @Override
    public boolean isReadOnly() {
        return false;
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
     * Retorna os parâmetros deste comando.
     * 
     * @return Array de parâmetros
     */
    public Object[] getParameters() {
        return parameters.clone();
    }
}

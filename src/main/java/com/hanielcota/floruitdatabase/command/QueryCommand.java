package com.hanielcota.floruitdatabase.command;

import com.hanielcota.floruitdatabase.exception.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Comando para execução de consultas SELECT no banco de dados.
 * 
 * <p>Este comando encapsula a execução de consultas de leitura, permitindo
 * mapeamento customizado dos resultados através de uma função handler.
 * 
 * @param <T> Tipo de objeto retornado pelo handler
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public final class QueryCommand<T> implements DatabaseCommand<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryCommand.class);
    
    private final String sql;
    private final Function<ResultSet, T> resultHandler;
    private final Object[] parameters;
    
    /**
     * Constrói um novo comando de consulta.
     * 
     * @param sql Query SQL a ser executada
     * @param resultHandler Função para mapear o ResultSet para o tipo T
     * @param parameters Parâmetros da query
     */
    private QueryCommand(String sql, Function<ResultSet, T> resultHandler, Object... parameters) {
        this.sql = Objects.requireNonNull(sql, "SQL não pode ser nulo");
        this.resultHandler = Objects.requireNonNull(resultHandler, "Handler não pode ser nulo");
        this.parameters = parameters != null ? parameters : new Object[0];
    }
    
    /**
     * Cria um novo comando de consulta.
     * 
     * @param sql Query SQL a ser executada
     * @param resultHandler Função para mapear o ResultSet para o tipo T
     * @param parameters Parâmetros da query
     * @param <T> Tipo de retorno
     * @return Novo comando de consulta
     */
    public static <T> QueryCommand<T> of(String sql, Function<ResultSet, T> resultHandler, Object... parameters) {
        return new QueryCommand<>(sql, resultHandler, parameters);
    }
    
    @Override
    public CompletableFuture<T> execute(Connection connection) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                
                // Define os parâmetros
                setParameters(statement, parameters);
                
                logger.debug("Executando query: {}", sql);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    T result = resultHandler.apply(resultSet);
                    logger.debug("Query executada com sucesso");
                    return result;
                }
                
            } catch (SQLException e) {
                logger.error("Erro ao executar query: {}", sql, e);
                throw new QueryException("Falha ao executar consulta", sql, e);
            }
        });
    }
    
    @Override
    public String getDescription() {
        return String.format("QueryCommand[%s]", sql.length() > 50 ? sql.substring(0, 50) + "..." : sql);
    }
    
    @Override
    public boolean isReadOnly() {
        return true;
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

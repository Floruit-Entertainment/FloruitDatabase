package com.hanielcota.floruitdatabase.exception;

/**
 * Exceção lançada quando ocorrem erros durante a execução de consultas SQL.
 * 
 * <p>Esta exceção é específica para erros relacionados à execução de queries,
 * problemas de sintaxe SQL, violações de constraints ou outros erros de banco.
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public class QueryException extends FloruitDatabaseException {
    
    private static final long serialVersionUID = 1L;
    
    private final String sql;
    
    /**
     * Constrói uma nova exceção de query com a mensagem e SQL especificados.
     * 
     * @param message Mensagem descritiva do erro
     * @param sql Query SQL que causou o erro
     */
    public QueryException(String message, String sql) {
        super(message);
        this.sql = sql;
    }
    
    /**
     * Constrói uma nova exceção de query com a mensagem, SQL e causa especificados.
     * 
     * @param message Mensagem descritiva do erro
     * @param sql Query SQL que causou o erro
     * @param cause Causa raiz da exceção
     */
    public QueryException(String message, String sql, Throwable cause) {
        super(message, cause);
        this.sql = sql;
    }
    
    /**
     * Constrói uma nova exceção de query com o SQL e causa especificados.
     * 
     * @param sql Query SQL que causou o erro
     * @param cause Causa raiz da exceção
     */
    public QueryException(String sql, Throwable cause) {
        super("Erro ao executar query: " + sql, cause);
        this.sql = sql;
    }
    
    /**
     * Retorna a query SQL que causou o erro.
     * 
     * @return Query SQL problemática
     */
    public String getSql() {
        return sql;
    }
}

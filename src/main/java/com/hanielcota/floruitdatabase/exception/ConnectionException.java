package com.hanielcota.floruitdatabase.exception;

/**
 * Exceção lançada quando ocorrem problemas de conexão com o banco de dados.
 * 
 * <p>Esta exceção é específica para erros relacionados à conectividade,
 * configuração de pool de conexões ou problemas de rede.
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public class ConnectionException extends FloruitDatabaseException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constrói uma nova exceção de conexão com a mensagem especificada.
     * 
     * @param message Mensagem descritiva do erro de conexão
     */
    public ConnectionException(String message) {
        super(message);
    }
    
    /**
     * Constrói uma nova exceção de conexão com a mensagem e causa especificadas.
     * 
     * @param message Mensagem descritiva do erro de conexão
     * @param cause Causa raiz da exceção
     */
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constrói uma nova exceção de conexão com a causa especificada.
     * 
     * @param cause Causa raiz da exceção
     */
    public ConnectionException(Throwable cause) {
        super(cause);
    }
}

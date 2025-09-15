package com.hanielcota.floruitdatabase.exception;

/**
 * Exceção base para todos os erros relacionados ao FloruitDatabase.
 * 
 * <p>Esta classe serve como base para todas as exceções específicas da biblioteca,
 * permitindo tratamento centralizado de erros e melhor rastreabilidade.
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public class FloruitDatabaseException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constrói uma nova exceção com a mensagem especificada.
     * 
     * @param message Mensagem descritiva do erro
     */
    public FloruitDatabaseException(String message) {
        super(message);
    }
    
    /**
     * Constrói uma nova exceção com a mensagem e causa especificadas.
     * 
     * @param message Mensagem descritiva do erro
     * @param cause Causa raiz da exceção
     */
    public FloruitDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constrói uma nova exceção com a causa especificada.
     * 
     * @param cause Causa raiz da exceção
     */
    public FloruitDatabaseException(Throwable cause) {
        super(cause);
    }
}

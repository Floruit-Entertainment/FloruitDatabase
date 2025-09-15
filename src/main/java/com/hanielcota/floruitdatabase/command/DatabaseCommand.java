package com.hanielcota.floruitdatabase.command;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;

/**
 * Interface base para todos os comandos de banco de dados.
 * 
 * <p>Implementa o padrão Command para encapsular operações de banco de dados
 * como objetos, permitindo enfileiramento, execução assíncrona e tratamento
 * uniforme de erros.
 * 
 * <p>Cada comando representa uma operação atômica que pode ser executada
 * de forma assíncrona e retorna um CompletableFuture com o resultado.
 * 
 * @param <T> Tipo de retorno do comando
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public interface DatabaseCommand<T> {
    
    /**
     * Executa o comando usando a conexão fornecida.
     * 
     * @param connection Conexão com o banco de dados
     * @return CompletableFuture que será completado com o resultado do comando
     * @throws Exception se ocorrer algum erro durante a execução
     */
    CompletableFuture<T> execute(Connection connection) throws Exception;
    
    /**
     * Retorna uma descrição do comando para logging e debugging.
     * 
     * @return Descrição do comando
     */
    String getDescription();
    
    /**
     * Verifica se o comando é uma operação de leitura (SELECT).
     * 
     * @return true se for uma operação de leitura, false caso contrário
     */
    default boolean isReadOnly() {
        return false;
    }
    
    /**
     * Verifica se o comando requer uma transação.
     * 
     * @return true se o comando requer transação, false caso contrário
     */
    default boolean requiresTransaction() {
        return false;
    }
}

package com.hanielcota.floruitdatabase.config;

/**
 * Representa a configuração imutável para a conexão com o banco de dados como um 'record' Java.
 *
 * @param host O endereço do servidor de banco de dados.
 * @param port A porta de conexão.
 * @param databaseName O nome do banco de dados.
 * @param username O nome de usuário para a conexão.
 * @param password A senha para a conexão.
 */
public record DatabaseConfig(String host, int port, String databaseName, String username, String password) {}

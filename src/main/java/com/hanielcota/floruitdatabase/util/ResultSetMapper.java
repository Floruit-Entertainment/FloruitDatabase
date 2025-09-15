package com.hanielcota.floruitdatabase.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Utilitário para mapeamento de ResultSet para objetos Java.
 * 
 * <p>Esta classe fornece métodos estáticos para facilitar o mapeamento
 * de resultados de consultas SQL para objetos Java, incluindo suporte
 * para mapeamento de registros únicos e listas.
 * 
 * @author Floruit Entertainment
 * @since 1.0.0
 */
public final class ResultSetMapper {
    
    private ResultSetMapper() {
        // Classe utilitária - não deve ser instanciada
    }
    
    /**
     * Mapeia o primeiro registro do ResultSet para um objeto.
     * 
     * @param resultSet ResultSet a ser mapeado
     * @param mapper Função de mapeamento
     * @param <T> Tipo do objeto de destino
     * @return Objeto mapeado ou null se não houver registros
     * @throws SQLException se ocorrer erro ao acessar o ResultSet
     */
    public static <T> T mapFirst(ResultSet resultSet, Function<ResultSet, T> mapper) throws SQLException {
        if (resultSet.next()) {
            return mapper.apply(resultSet);
        }
        return null;
    }
    
    /**
     * Mapeia todos os registros do ResultSet para uma lista de objetos.
     * 
     * @param resultSet ResultSet a ser mapeado
     * @param mapper Função de mapeamento
     * @param <T> Tipo dos objetos de destino
     * @return Lista de objetos mapeados
     * @throws SQLException se ocorrer erro ao acessar o ResultSet
     */
    public static <T> List<T> mapAll(ResultSet resultSet, Function<ResultSet, T> mapper) throws SQLException {
        List<T> results = new ArrayList<>();
        
        while (resultSet.next()) {
            results.add(mapper.apply(resultSet));
        }
        
        return results;
    }
    
    /**
     * Mapeia um ResultSet para um objeto Record simples.
     * 
     * @param resultSet ResultSet a ser mapeado
     * @param recordClass Classe do Record
     * @param <T> Tipo do Record
     * @return Record mapeado ou null se não houver registros
     * @throws SQLException se ocorrer erro ao acessar o ResultSet
     */
    public static <T> T mapToRecord(ResultSet resultSet, Class<T> recordClass) throws SQLException {
        if (!resultSet.next()) {
            return null;
        }
        
        // Esta é uma implementação simplificada
        // Em uma implementação real, seria necessário usar reflection
        // ou uma biblioteca como MapStruct para mapeamento automático
        throw new UnsupportedOperationException("Mapeamento automático de Records não implementado");
    }
    
    /**
     * Mapeia um ResultSet para uma lista de Records.
     * 
     * @param resultSet ResultSet a ser mapeado
     * @param recordClass Classe do Record
     * @param <T> Tipo do Record
     * @return Lista de Records mapeados
     * @throws SQLException se ocorrer erro ao acessar o ResultSet
     */
    public static <T> List<T> mapToRecordList(ResultSet resultSet, Class<T> recordClass) throws SQLException {
        List<T> results = new ArrayList<>();
        
        while (resultSet.next()) {
            // Implementação simplificada - em produção seria necessário reflection
            throw new UnsupportedOperationException("Mapeamento automático de Records não implementado");
        }
        
        return results;
    }
    
    /**
     * Mapeia um ResultSet para um mapa de coluna-valor.
     * 
     * @param resultSet ResultSet a ser mapeado
     * @return Mapa com os valores da primeira linha
     * @throws SQLException se ocorrer erro ao acessar o ResultSet
     */
    public static java.util.Map<String, Object> mapToMap(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return java.util.Collections.emptyMap();
        }
        
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        var metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            Object value = resultSet.getObject(i);
            map.put(columnName, value);
        }
        
        return map;
    }
    
    /**
     * Mapeia um ResultSet para uma lista de mapas.
     * 
     * @param resultSet ResultSet a ser mapeado
     * @return Lista de mapas com os valores de cada linha
     * @throws SQLException se ocorrer erro ao acessar o ResultSet
     */
    public static List<java.util.Map<String, Object>> mapToListOfMaps(ResultSet resultSet) throws SQLException {
        List<java.util.Map<String, Object>> results = new ArrayList<>();
        
        while (resultSet.next()) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            var metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = resultSet.getObject(i);
                map.put(columnName, value);
            }
            
            results.add(map);
        }
        
        return results;
    }
}

# 💾 FloruitDatabase

Uma biblioteca Java moderna e de alta performance para interação com bancos de dados MySQL, desenvolvida especificamente para aproveitar as funcionalidades mais recentes do **Java 21**.

## ✨ Características Principais

- ☕ **Java 21 & Virtual Threads** - Aproveita o Project Loom para máxima concorrência
- 🚀 **Pool de Conexões HikariCP** - Performance otimizada com configurações avançadas
- 🏛️ **Design Patterns Modernos** - Builder, Command, Facade e Singleton
- ⚡ **Operações Assíncronas** - Todas as operações usam CompletableFuture
- 🔒 **Segurança e Robustez** - Java Records, validações e tratamento de erros
- 🧩 **Framework-Independent** - Funciona com qualquer aplicação Java

## 🚀 Instalação

### Gradle

```gradle
dependencies {
    implementation 'com.hanielcota:floruitdatabase:1.0.0'
    implementation 'com.zaxxer:HikariCP:6.3.0'
    implementation 'mysql:mysql-connector-java:8.0.33'
}
```

### Maven

```xml
<dependency>
    <groupId>com.hanielcota</groupId>
    <artifactId>floruitdatabase</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 📖 Uso Básico

### Configuração

```java
import com.hanielcota.floruitdatabase.FloruitDatabase;
import com.hanielcota.floruitdatabase.config.DatabaseConfig;

// Configuração usando Builder pattern
DatabaseConfig config = DatabaseConfig.builder("localhost", "meu_banco", "usuario")
    .password("senha_segura")
    .port(3306)
    .maxPoolSize(20)
    .minIdle(5)
    .build();
```

### Operações Básicas

```java
try (FloruitDatabase db = new FloruitDatabase(config)) {
    
    // Inserção assíncrona
    CompletableFuture<Integer> result = db.executeUpdate(
        "INSERT INTO usuarios (nome, email) VALUES (?, ?)",
        "João Silva", "joao@example.com"
    );
    
    int linhasAfetadas = result.join();
    System.out.println("Usuário inserido: " + linhasAfetadas);
    
    // Consulta com mapeamento
    CompletableFuture<Usuario> usuario = db.executeQuery(
        "SELECT * FROM usuarios WHERE email = ?",
        rs -> new Usuario(rs.getInt("id"), rs.getString("nome"), rs.getString("email")),
        "joao@example.com"
    );
    
    Usuario user = usuario.join();
    System.out.println("Usuário encontrado: " + user);
}
```

### Operações em Lote

```java
// Preparar dados para inserção em lote
List<Object[]> dados = List.of(
    new Object[]{"Maria Silva", "maria@example.com"},
    new Object[]{"Pedro Santos", "pedro@example.com"},
    new Object[]{"Ana Costa", "ana@example.com"}
);

// Executar inserção em lote
CompletableFuture<int[]> resultado = db.executeBatch(
    "INSERT INTO usuarios (nome, email) VALUES (?, ?)",
    dados
);

int[] resultados = resultado.join();
int totalInserido = Arrays.stream(resultados).sum();
System.out.println("Total inserido: " + totalInserido);
```

### Transações

```java
// Transação atômica
CompletableFuture<Void> transacao = db.executeTransaction(
    // Inserir usuário
    UpdateCommand.of("INSERT INTO usuarios (nome, email) VALUES (?, ?)", 
                    "João", "joao@example.com"),
    
    // Inserir pedido
    UpdateCommand.of("INSERT INTO pedidos (usuario_id, produto) VALUES (?, ?)", 
                    1, "Produto A")
);

transacao.join();
System.out.println("Transação executada com sucesso");
```

### Fila de Comandos

```java
// Enfileirar comandos para execução sequencial
CompletableFuture<Integer> resultado1 = db.enqueueCommand(
    UpdateCommand.of("INSERT INTO logs (mensagem) VALUES (?)", "Log 1")
);

CompletableFuture<Integer> resultado2 = db.enqueueCommand(
    UpdateCommand.of("INSERT INTO logs (mensagem) VALUES (?)", "Log 2")
);

// Os comandos serão executados sequencialmente
resultado1.join();
resultado2.join();
```

## 🏗️ Arquitetura

### Design Patterns Utilizados

1. **Builder Pattern** - `DatabaseConfigBuilder` para configuração fluente
2. **Command Pattern** - `DatabaseCommand` e suas implementações
3. **Facade Pattern** - `FloruitDatabase` como interface unificada
4. **Singleton Pattern** - Gerenciamento de recursos compartilhados

### Componentes Principais

- **`FloruitDatabase`** - API principal (Facade)
- **`ConnectionManager`** - Gerenciamento do pool HikariCP
- **`AsyncExecutor`** - Execução com Virtual Threads
- **`CommandQueue`** - Fila de comandos sequenciais
- **`DatabaseCommand`** - Comandos encapsulados

## ⚡ Performance

### Virtual Threads (Java 21)

```java
// Suporte nativo a milhares de operações concorrentes
CompletableFuture.allOf(
    db.executeUpdate("INSERT INTO logs (msg) VALUES (?)", "Msg 1"),
    db.executeUpdate("INSERT INTO logs (msg) VALUES (?)", "Msg 2"),
    db.executeUpdate("INSERT INTO logs (msg) VALUES (?)", "Msg 3")
    // ... milhares de operações
).join();
```

### Pool de Conexões Otimizado

```java
DatabaseConfig config = DatabaseConfig.builder("localhost", "db", "user")
    .password("pass")
    .maxPoolSize(20)           // Pool otimizado
    .minIdle(5)               // Conexões mínimas
    .connectionTimeout(Duration.ofSeconds(30))
    .idleTimeout(Duration.ofMinutes(10))
    .cachePrepStmts(true)     // Cache de prepared statements
    .prepStmtCacheSize(250)   // Tamanho do cache
    .build();
```

## 🔧 Configuração Avançada

### Configurações de Performance

```java
DatabaseConfig config = DatabaseConfig.builder("localhost", "db", "user")
    .password("pass")
    .maxPoolSize(50)                    // Pool maior para alta carga
    .minIdle(10)                       // Mais conexões idle
    .connectionTimeout(Duration.ofSeconds(10))  // Timeout menor
    .idleTimeout(Duration.ofMinutes(5))         // Idle timeout menor
    .maxLifetime(Duration.ofMinutes(30))        // Vida útil das conexões
    .leakDetectionThreshold(Duration.ofSeconds(5)) // Detecção de vazamentos
    .cachePrepStmts(true)              // Cache habilitado
    .prepStmtCacheSize(500)            // Cache maior
    .prepStmtCacheSqlLimit(4096)       // Limite maior para SQL
    .build();
```

### Monitoramento

```java
// Informações do sistema
var info = db.getInfo();
System.out.println("Status: " + info.getStatus());
System.out.println("Pool: " + info.poolInfo().activeConnections() + "/" + info.poolInfo().maxPoolSize());
System.out.println("Fila: " + info.queueInfo().queueSize() + " pendentes");
System.out.println("Taxa de sucesso: " + (info.queueInfo().getSuccessRate() * 100) + "%");

// Verificar saúde do sistema
if (db.isHealthy()) {
    System.out.println("Sistema saudável");
}
```

## 🛠️ Comandos Personalizados

### Criando Comandos Customizados

```java
public class CustomQueryCommand<T> implements DatabaseCommand<T> {
    private final String sql;
    private final Function<ResultSet, T> mapper;
    private final Object[] params;
    
    public CustomQueryCommand(String sql, Function<ResultSet, T> mapper, Object... params) {
        this.sql = sql;
        this.mapper = mapper;
        this.params = params;
    }
    
    @Override
    public CompletableFuture<T> execute(Connection connection) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                // Configurar parâmetros
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return mapper.apply(rs);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public String getDescription() {
        return "CustomQuery[" + sql + "]";
    }
}
```

## 🚨 Tratamento de Erros

### Exceções Específicas

```java
try (FloruitDatabase db = new FloruitDatabase(config)) {
    // Operações do banco
} catch (ConnectionException e) {
    // Erro de conexão
    logger.error("Falha na conexão: {}", e.getMessage());
} catch (QueryException e) {
    // Erro na query
    logger.error("Erro na query '{}': {}", e.getSql(), e.getMessage());
} catch (FloruitDatabaseException e) {
    // Erro geral
    logger.error("Erro no banco de dados: {}", e.getMessage());
}
```

## 📊 Exemplo Completo

Veja o arquivo `demo/FloruitDatabaseDemo.java` para um exemplo completo de uso da biblioteca, incluindo:

- Operações CRUD básicas
- Operações em lote
- Transações atômicas
- Consultas complexas
- Monitoramento do sistema

## 🤝 Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 👥 Autores

- **Floruit Entertainment** - *Desenvolvimento inicial* - [FloruitEntertainment](https://github.com/FloruitEntertainment)

## 🙏 Agradecimentos

- [HikariCP](https://github.com/brettwooldridge/HikariCP) - Pool de conexões de alta performance
- [Project Loom](https://openjdk.org/projects/loom/) - Virtual Threads do Java 21
- Comunidade Java por feedback e sugestões

---

© 2025 Floruit Entertainment LTDA. FloruitDatabase é uma marca registrada da Floruit Entertainment. Todos os direitos reservados.

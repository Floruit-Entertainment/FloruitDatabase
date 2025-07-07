# 💾 FloruitDB

A modern, **high-performance**, and user-friendly Java database library, designed with best practices and the latest features of **Java 21**. Optimized for asynchronous operations and scalability, it is ideal for applications requiring low latency and high concurrency.

`FloruitDB` simplifies JDBC usage and connection management, offering a fluent and secure API for interacting with **MySQL** databases, prioritizing **maximum performance** and ease of maintenance.

---

## ✨ Key Features

- ☕ **High Performance with Java 21 & Virtual Threads**  
  Leverages **Project Loom** to support thousands of concurrent queries with efficient resource usage, ensuring massive scalability in asynchronous I/O operations.

- 🚀 **Ultra-Fast Connection Pool**  
  Integrated with **HikariCP**, the most performant connection pool available, optimized for minimal latency and high throughput.

- 🏛️ **Modern Architecture with Design Patterns**  
  - **Builder**: Fluent and secure configuration with `DatabaseConfigBuilder`.  
  - **Command**: Asynchronous task queue for processing large volumes of updates with control and efficiency.

- ⚡ **Fully Asynchronous**  
  All operations use `CompletableFuture`, enabling **non-blocking** and highly reactive code flow, ideal for high-performance applications.

- 🔒 **Security and Robustness**  
  Utilizes **Java Records** for immutability, strict nullability checks, and reliable transaction management.

- 🧩 **Framework-Independent**  
  Works with any Java application, without dependencies on frameworks like Spring or others.

---

## 🚀 Install

### Maven

```xml
<dependency>
    <groupId>com.seusite.floruitdb</groupId>
    <artifactId>floruitdb-api</artifactId>
    <version>2.0.0-FINAL</version>
</dependency>

implementation 'com.seusite.floruitdb:floruitdb-api:2.0.0-FINAL'

import com.seusite.floruitdb.FloruitDB;
import com.seusite.floruitdb.config.DatabaseConfig;
import com.seusite.floruitdb.config.DatabaseConfigBuilder;

public class MainApplication {

    public static void main(String[] args) {
        // Configuração do banco de dados
        DatabaseConfig config = new DatabaseConfigBuilder(
                "localhost",
                "meu_banco",
                "usuario_db")
                .port(3306)
                .password("senha_segura")
                .build();

        // Inicialização e uso assíncrono
        try (FloruitDB database = new FloruitDB(config)) {
            String sql = "CREATE TABLE IF NOT EXISTS usuarios (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(100) NOT NULL);";
            database.executeUpdate(sql)
                    .thenAccept(result -> System.out.println("Tabela criada com sucesso!"))
                    .exceptionally(throwable -> {
                        throwable.printStackTrace();
                        return null;
                    })
                    .join(); // Aguarda a conclusão da operação assíncrona
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## ⚡ Extreme Focus on Speed and Async

- **Non-Blocking Operations**: All database interactions are **asynchronous**, using `CompletableFuture` for instant responses and **maximum responsiveness**.  
- **Project Loom Virtual Threads**: Reduces thread overhead, enabling **thousands of simultaneous operations** with minimal resource usage.  
- **Hyper-Optimized HikariCP**: Advanced configurations ensure **ultra-fast response times**, even in high-load scenarios.  
- **Extreme Scalability**: Designed to handle **traffic spikes** and large data volumes, maintaining **stable performance** and **minimal latency**.

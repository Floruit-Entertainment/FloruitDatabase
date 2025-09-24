# 💾 FloruitDatabase

A modern, high-performance Java library for interacting with MySQL databases, specifically designed to leverage the latest features of **Java 21**.

## ✨ Key Features

  - ☕ **Java 21 & Virtual Threads** - Leverages Project Loom for maximum concurrency
  - 🚀 **HikariCP Connection Pool** - Optimized performance with advanced configurations
  - 🏛️ **Modern Design Patterns** - Builder, Command, Facade, and Singleton
  - ⚡ **Asynchronous Operations** - All operations use `CompletableFuture`
  - 🔒 **Security and Robustness** - Java Records, validations, and error handling
  - 🧩 **Framework-Independent** - Works with any Java application

## 🚀 Installation

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

## 📖 Basic Usage

### Configuration

```java
import com.hanielcota.floruitdatabase.FloruitDatabase;
import com.hanielcota.floruitdatabase.config.DatabaseConfig;

// Configuration using the Builder pattern
DatabaseConfig config = DatabaseConfig.builder("localhost", "my_database", "user")
    .password("secure_password")
    .port(3306)
    .maxPoolSize(20)
    .minIdle(5)
    .build();
```

### Basic Operations

```java
try (FloruitDatabase db = new FloruitDatabase(config)) {
    
    // Asynchronous insertion
    CompletableFuture<Integer> result = db.executeUpdate(
        "INSERT INTO users (name, email) VALUES (?, ?)",
        "John Doe", "john@example.com"
    );
    
    int affectedRows = result.join();
    System.out.println("User inserted: " + affectedRows);
    
    // Query with mapping
    CompletableFuture<User> userFuture = db.executeQuery(
        "SELECT * FROM users WHERE email = ?",
        rs -> new User(rs.getInt("id"), rs.getString("name"), rs.getString("email")),
        "john@example.com"
    );
    
    User user = userFuture.join();
    System.out.println("User found: " + user);
}
```

### Batch Operations

```java
// Prepare data for batch insertion
List<Object[]> data = List.of(
    new Object[]{"Maria Silva", "maria@example.com"},
    new Object[]{"Pedro Santos", "pedro@example.com"},
    new Object[]{"Ana Costa", "ana@example.com"}
);

// Execute batch insert
CompletableFuture<int[]> result = db.executeBatch(
    "INSERT INTO users (name, email) VALUES (?, ?)",
    data
);

int[] results = result.join();
int totalInserted = Arrays.stream(results).sum();
System.out.println("Total inserted: " + totalInserted);
```

### Transactions

```java
// Atomic transaction
CompletableFuture<Void> transaction = db.executeTransaction(
    // Insert user
    UpdateCommand.of("INSERT INTO users (name, email) VALUES (?, ?)", 
                     "John", "john@example.com"),
    
    // Insert order
    UpdateCommand.of("INSERT INTO orders (user_id, product) VALUES (?, ?)", 
                     1, "Product A")
);

transaction.join();
System.out.println("Transaction executed successfully");
```

### Command Queue

```java
// Enqueue commands for sequential execution
CompletableFuture<Integer> result1 = db.enqueueCommand(
    UpdateCommand.of("INSERT INTO logs (message) VALUES (?)", "Log 1")
);

CompletableFuture<Integer> result2 = db.enqueueCommand(
    UpdateCommand.of("INSERT INTO logs (message) VALUES (?)", "Log 2")
);

// The commands will be executed sequentially
result1.join();
result2.join();
```

## 🏗️ Architecture

### Design Patterns Used

1.  **Builder Pattern** - `DatabaseConfigBuilder` for fluent configuration
2.  **Command Pattern** - `DatabaseCommand` and its implementations
3.  **Facade Pattern** - `FloruitDatabase` as a unified interface
4.  **Singleton Pattern** - Management of shared resources

### Core Components

  - **`FloruitDatabase`** - Main API (Facade)
  - **`ConnectionManager`** - HikariCP pool management
  - **`AsyncExecutor`** - Execution with Virtual Threads
  - **`CommandQueue`** - Sequential command queue
  - **`DatabaseCommand`** - Encapsulated commands

## ⚡ Performance

### Virtual Threads (Java 21)

```java
// Native support for thousands of concurrent operations
CompletableFuture.allOf(
    db.executeUpdate("INSERT INTO logs (msg) VALUES (?)", "Msg 1"),
    db.executeUpdate("INSERT INTO logs (msg) VALUES (?)", "Msg 2"),
    db.executeUpdate("INSERT INTO logs (msg) VALUES (?)", "Msg 3")
    // ... thousands of operations
).join();
```

### Optimized Connection Pool

```java
DatabaseConfig config = DatabaseConfig.builder("localhost", "db", "user")
    .password("pass")
    .maxPoolSize(20)              // Optimized pool
    .minIdle(5)                   // Minimum idle connections
    .connectionTimeout(Duration.ofSeconds(30))
    .idleTimeout(Duration.ofMinutes(10))
    .cachePrepStmts(true)         // Cache for prepared statements
    .prepStmtCacheSize(250)       // Cache size
    .build();
```

## 🔧 Advanced Configuration

### Performance Settings

```java
DatabaseConfig config = DatabaseConfig.builder("localhost", "db", "user")
    .password("pass")
    .maxPoolSize(50)                    // Larger pool for high load
    .minIdle(10)                        // More idle connections
    .connectionTimeout(Duration.ofSeconds(10)) // Shorter timeout
    .idleTimeout(Duration.ofMinutes(5))       // Shorter idle timeout
    .maxLifetime(Duration.ofMinutes(30))      // Connection lifetime
    .leakDetectionThreshold(Duration.ofSeconds(5)) // Leak detection
    .cachePrepStmts(true)               // Cache enabled
    .prepStmtCacheSize(500)             // Larger cache
    .prepStmtCacheSqlLimit(4096)        // Higher limit for SQL
    .build();
```

### Monitoring

```java
// System information
var info = db.getInfo();
System.out.println("Status: " + info.getStatus());
System.out.println("Pool: " + info.poolInfo().activeConnections() + "/" + info.poolInfo().maxPoolSize());
System.out.println("Queue: " + info.queueInfo().queueSize() + " pending");
System.out.println("Success rate: " + (info.queueInfo().getSuccessRate() * 100) + "%");

// Check system health
if (db.isHealthy()) {
    System.out.println("System is healthy");
}
```

## 🛠️ Custom Commands

### Creating Custom Commands

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
                // Set parameters
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

## 🚨 Error Handling

### Specific Exceptions

```java
try (FloruitDatabase db = new FloruitDatabase(config)) {
    // Database operations
} catch (ConnectionException e) {
    // Connection error
    logger.error("Connection failed: {}", e.getMessage());
} catch (QueryException e) {
    // Query error
    logger.error("Error in query '{}': {}", e.getSql(), e.getMessage());
} catch (FloruitDatabaseException e) {
    // General error
    logger.error("Database error: {}", e.getMessage());
}
```

## 📊 Complete Example

See the `demo/FloruitDatabaseDemo.java` file for a complete example of how to use the library, including:

  - Basic CRUD operations
  - Batch operations
  - Atomic transactions
  - Complex queries
  - System monitoring

## 🤝 Contribution

1.  Fork the project
2.  Create a branch for your feature (`git checkout -b feature/AmazingFeature`)
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](https://www.google.com/search?q=LICENSE) file for details.

## 👥 Authors

  - **Floruit Entertainment** - *Initial development* - [FloruitEntertainment](https://github.com/FloruitEntertainment)

## 🙏 Acknowledgements

  - [HikariCP](https://github.com/brettwooldridge/HikariCP) - High-performance connection pool
  - [Project Loom](https://openjdk.org/projects/loom/) - Java 21's Virtual Threads
  - The Java community for feedback and suggestions

-----

© 2025 Floruit Entertainment LTD. FloruitDatabase is a registered trademark of Floruit Entertainment. All rights reserved.

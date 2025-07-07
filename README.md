# 💾 FloruitDB

Uma biblioteca de banco de dados Java moderna, de **alta performance** e fácil de usar, projetada com as melhores práticas de design e os recursos mais recentes do **Java 21**. Otimizada para operações assíncronas e escalabilidade, é ideal para aplicações que exigem baixa latência e alta concorrência.

`FloruitDB` simplifica o uso do JDBC e o gerenciamento de conexões, oferecendo uma API fluente e segura para interagir com bancos de dados **MySQL**, priorizando **máxima performance** e facilidade de manutenção.

---

## ✨ Principais Características

- ☕ **Alta Performance com Java 21 & Threads Virtuais**  
  Aproveita o **Project Loom** para suportar milhares de queries concorrentes com uso eficiente de recursos, garantindo escalabilidade massiva em operações de I/O assíncronas.

- 🚀 **Pool de Conexões Ultrarrápido**  
  Integrado com **HikariCP**, o pool de conexões mais performático do mercado, otimizado para mínima latência e alta taxa de transferência.

- 🏛️ **Arquitetura Moderna com Design Patterns**  
  - **Builder**: Configuração fluida e segura com `DatabaseConfigBuilder`.  
  - **Command**: Fila de tarefas assíncrona para processar grandes volumes de atualizações com controle e eficiência.

- ⚡ **Totalmente Assíncrona**  
  Todas as operações utilizam `CompletableFuture`, permitindo um fluxo de código **não-bloqueante** e altamente reativo, ideal para aplicações de alta performance.

- 🔒 **Segurança e Robustez**  
  Usa **Java Records** para imutabilidade, validações rigorosas de nulidade e gerenciamento confiável de transações.

- 🧩 **Independente de Frameworks**  
  Funciona em qualquer aplicação Java, sem dependências de frameworks como Spring ou outros.

---

## 🚀 Instalação

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


package com.example.transaction.it;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Testcontainers
@SpringBootTest
@ActiveProfiles("sharding-it")
public abstract class AbstractShardingIntegrationTest {
    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Container
    static final PostgreSQLContainer<?> SHARD_0 = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("transaction")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static final PostgreSQLContainer<?> SHARD_1 = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("transaction")
            .withUsername("postgres")
            .withPassword("postgres");

    @BeforeAll
    static void applySchema() throws Exception {
        String initSql = new String(
                AbstractShardingIntegrationTest.class
                        .getClassLoader()
                        .getResourceAsStream("init-shard.sql")
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );
        for (PostgreSQLContainer<?> shard : new PostgreSQLContainer<?>[]{SHARD_0, SHARD_1}) {
            try (Connection conn = shard.createConnection("");
                 Statement stmt = conn.createStatement()) {
                stmt.execute(initSql);
            }
        }
    }

    @DynamicPropertySource
    static void registerShardUrls(DynamicPropertyRegistry registry) {
        registry.add("test.shard0.url", SHARD_0::getJdbcUrl);
        registry.add("test.shard0.username", SHARD_0::getUsername);
        registry.add("test.shard0.password", SHARD_0::getPassword);
        registry.add("test.shard1.url", SHARD_1::getJdbcUrl);
        registry.add("test.shard1.username", SHARD_1::getUsername);
        registry.add("test.shard1.password", SHARD_1::getPassword);
    }

    // ── DataSource, собранный из двух тест-контейнеров ─────────

    @Configuration
    @Profile("sharding-it")
    static class ShardingTestDataSourceConfig {

        @Bean
        @Primary
        DataSource dataSource(Environment env) throws SQLException, IOException {
            String yaml = buildYaml(
                    env.getProperty("test.shard0.url"),
                    env.getProperty("test.shard0.username"),
                    env.getProperty("test.shard0.password"),
                    env.getProperty("test.shard1.url"),
                    env.getProperty("test.shard1.username"),
                    env.getProperty("test.shard1.password")
            );

            File tmp = File.createTempFile("sharding-it", ".yaml");
            tmp.deleteOnExit();
            try (PrintWriter pw = new PrintWriter(tmp, StandardCharsets.UTF_8)) {
                pw.print(yaml);
            }
            return YamlShardingSphereDataSourceFactory.createDataSource(tmp);
        }

        private String buildYaml(String url0, String user0, String pass0,
                                 String url1, String user1, String pass1) {
            return ("""
            dataSources:
              ds_0:
                dataSourceClassName: com.zaxxer.hikari.HikariDataSource
                driverClassName: org.postgresql.Driver
                jdbcUrl: %s
                username: %s
                password: %s
                connectionTimeout: 30000
                maximumPoolSize: 5
              ds_1:
                dataSourceClassName: com.zaxxer.hikari.HikariDataSource
                driverClassName: org.postgresql.Driver
                jdbcUrl: %s
                username: %s
                password: %s
                connectionTimeout: 30000
                maximumPoolSize: 5

            rules:
              - !SHARDING
                tables:
                  transactions:
                    actualDataNodes: ds_${0..1}.transactions
                    databaseStrategy:
                      standard:
                        shardingColumn: user_uid
                        shardingAlgorithmName: user_uid_hash
                    keyGenerateStrategy:
                      column: uid
                      keyGeneratorName: uuid
                  wallets:
                    actualDataNodes: ds_${0..1}.wallets
                    databaseStrategy:
                      standard:
                        shardingColumn: user_uid
                        shardingAlgorithmName: user_uid_hash
                    keyGenerateStrategy:
                      column: uid
                      keyGeneratorName: uuid

                shardingAlgorithms:
                  user_uid_hash:
                    type: INLINE
                    props:
                      algorithm-expression: "ds_${Math.abs(user_uid.hashCode()) %% 2}"

                keyGenerators:
                  uuid:
                    type: UUID

              - !BROADCAST
                tables:
                  - wallet_types

            props:
              sql-show: true
            """).formatted(url0, user0, pass0, url1, user1, pass1);
        }
    }
}

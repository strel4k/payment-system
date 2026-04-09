package com.example.webhookcollector.it.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIT {

    static final PostgreSQLContainer<?> POSTGRES;
    public static final KafkaContainer KAFKA;

    public static final String TEST_WEBHOOK_TOKEN = "test-webhook-token";
    public static final String TEST_HMAC_SECRET   = "test-hmac-secret";

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("webhook_test")
                .withUsername("webhook_test")
                .withPassword("webhook_test");
        POSTGRES.start();

        KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
        KAFKA.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",          POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",     POSTGRES::getUsername);
        registry.add("spring.datasource.password",     POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("webhook.security.token",         () -> TEST_WEBHOOK_TOKEN);
        registry.add("webhook.security.hmac-secret",   () -> TEST_HMAC_SECRET);
    }
}
package com.example.individualsapi.it;

import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AuthFlowIntegrationTest {

    @BeforeAll
    static void disableHttpsRequirementForTestKeycloak() throws Exception {keycloak.execInContainer("sh", "-lc",
                "/opt/keycloak/bin/kcadm.sh config credentials " +
                        "--server http://localhost:8080 " +
                        "--realm master " +
                        "--user admin --password admin && " +
                        "/opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE && " +
                        "/opt/keycloak/bin/kcadm.sh update realms/individuals -s sslRequired=NONE"
        );
    }

    @Autowired
    private WebTestClient webTestClient;

    @Container
    static final GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:26.2")
            .withExposedPorts(8080)
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
            .withEnv("KC_HEALTH_ENABLED", "true")
            .withEnv("KC_METRICS_ENABLED", "true")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("realm-config.json"),
                    "/opt/keycloak/data/import/realm-config.json"
            )
            .withCommand("start-dev", "--import-realm")
            .waitingFor(
                    Wait.forHttp("/")
                            .forPort(8080)
                            .withStartupTimeout(Duration.ofMinutes(2))
            );

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        String baseUrl = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080);

        r.add("keycloak.base-url", () -> baseUrl);
        r.add("keycloak.realm", () -> "individuals");
        r.add("keycloak.client-id", () -> "individuals-client");
        r.add("keycloak.client-secret", () -> "secret");
        r.add("keycloak.admin.username", () -> "admin");
        r.add("keycloak.admin.password", () -> "admin");

        r.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> baseUrl + "/realms/individuals");
    }

    @Test
    @DisplayName("Registration -> Me: создаём пользователя в Keycloak и читаем /me по access_token")
    void registration_then_me_returns_user_info() {

        String email = "it-user-" + System.currentTimeMillis() + "@example.com";
        String password = "Qwe12345!";

        String registrationJson = """
                {
                  "email": "%s",
                  "password": "%s",
                  "confirm_password": "%s"
                }
                """.formatted(email, password, password);

        TokenResponse regTokens = webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registrationJson)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(TokenResponse.class)
                .returnResult()
                .getResponseBody();

        org.junit.jupiter.api.Assertions.assertNotNull(regTokens);
        org.junit.jupiter.api.Assertions.assertNotNull(regTokens.getAccessToken());
        org.junit.jupiter.api.Assertions.assertFalse(regTokens.getAccessToken().isBlank());

        UserInfoResponse me = webTestClient.get()
                .uri("/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + regTokens.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(UserInfoResponse.class)
                .returnResult()
                .getResponseBody();

        org.junit.jupiter.api.Assertions.assertNotNull(me);
        org.junit.jupiter.api.Assertions.assertNotNull(me.getId());
        org.junit.jupiter.api.Assertions.assertEquals(email, me.getEmail());
    }

    @Test
    @DisplayName("Login: после регистрации логинимся через password grant и получаем access_token")
    void login_returns_tokens() {

        String email = "it-login-" + System.currentTimeMillis() + "@example.com";
        String password = "Qwe12345!";

        String registrationJson = """
                {
                  "email": "%s",
                  "password": "%s",
                  "confirm_password": "%s"
                }
                """.formatted(email, password, password);

        webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registrationJson)
                .exchange()
                .expectStatus().isCreated();

        String loginJson = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        TokenResponse loginTokens = webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginJson)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(TokenResponse.class)
                .returnResult()
                .getResponseBody();

        org.junit.jupiter.api.Assertions.assertNotNull(loginTokens);
        org.junit.jupiter.api.Assertions.assertNotNull(loginTokens.getAccessToken());
        org.junit.jupiter.api.Assertions.assertFalse(loginTokens.getAccessToken().isBlank());
    }
}
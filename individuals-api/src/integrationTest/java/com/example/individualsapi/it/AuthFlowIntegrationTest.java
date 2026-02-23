package com.example.individualsapi.it;

import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.person.PersonResponse;
import com.example.individualsapi.client.PersonServiceClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AuthFlowIntegrationTest {

    @BeforeAll
    static void disableHttpsRequirementForTestKeycloak() throws Exception {
        keycloak.execInContainer("sh", "-lc",
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

    @MockBean
    private PersonServiceClient personServiceClient;

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
                    Wait.forHttp("/realms/individuals")
                            .forPort(8080)
                            .withStartupTimeout(Duration.ofMinutes(5))
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

    // ------------ настраивает мок person-service перед каждым вызовом ------------

    private void stubPersonService() {
        when(personServiceClient.createPerson(any()))
                .thenReturn(Mono.just(
                        new PersonResponse()
                                .userId(UUID.randomUUID())
                                .email("stub@example.com")
                ));
    }

    // ------------ 1. Registration → /me ------------

    @Test
    @DisplayName("Registration -> Me: создаём пользователя и читаем /me по access_token")
    void registration_then_me_returns_user_info() {
        stubPersonService();

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

        assertNotNull(regTokens);
        assertNotNull(regTokens.getAccessToken());
        assertFalse(regTokens.getAccessToken().isBlank());

        UserInfoResponse me = webTestClient.get()
                .uri("/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + regTokens.getAccessToken())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(UserInfoResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(me);
        assertNotNull(me.getId());
        assertEquals(email, me.getEmail());
    }

    // ------------ 2. Registration → Login ------------

    @Test
    @DisplayName("Login: после регистрации логинимся через password grant")
    void login_returns_tokens() {
        stubPersonService();

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

        assertNotNull(loginTokens);
        assertNotNull(loginTokens.getAccessToken());
        assertFalse(loginTokens.getAccessToken().isBlank());
    }

    // ------------ 3. Duplicate registration → person-service conflict ------------

    @Test
    @DisplayName("Duplicate registration → person-service возвращает 409")
    void duplicate_registration_returns_409() {
        String email = "it-dup-" + System.currentTimeMillis() + "@example.com";
        String password = "Qwe12345!";

        // Первый вызов — успех
        when(personServiceClient.createPerson(any()))
                .thenReturn(Mono.just(
                        new PersonResponse()
                                .userId(UUID.randomUUID())
                                .email(email)
                ));

        String json = """
                {
                  "email": "%s",
                  "password": "%s",
                  "confirm_password": "%s"
                }
                """.formatted(email, password, password);

        // первая регистрация
        webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isCreated();

        // Второй вызов — person-service видит дубликат по email
        when(personServiceClient.createPerson(any()))
                .thenReturn(Mono.error(
                        new org.springframework.web.reactive.function.client.WebClientResponseException(
                                409, "Conflict", null, null, null
                        )
                ));

        // вторая регистрация — должна упасть на person-service
        webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().is5xxServerError(); // individuals-api пробросит ошибку 500
    }
}
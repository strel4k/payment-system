package com.example.individualsapi.it;

import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.UserLoginRequest;
import com.example.dto.UserRegistrationRequest;
import com.example.individualsapi.service.TokenService;
import com.example.individualsapi.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
class AuthFlowIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenService tokenService;

    @Test
    @DisplayName("Регистрация: /v1/auth/registration возвращает токены")
    void registrationFlow_returnsTokenResponse() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("new-user@example.com");
        request.setPassword("Qwe12345!");
        request.setConfirmPassword("Qwe12345!");

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("reg-access");
        tokenResponse.setRefreshToken("reg-refresh");
        tokenResponse.setExpiresIn(300);
        tokenResponse.setTokenType("Bearer");

        when(userService.register(any(UserRegistrationRequest.class)))
                .thenReturn(Mono.just(tokenResponse));

        webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.access_token").isEqualTo("reg-access")
                .jsonPath("$.refresh_token").isEqualTo("reg-refresh")
                .jsonPath("$.token_type").isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Логин: /v1/auth/login возвращает токены")
    void loginFlow_returnsTokenResponse() {
        UserLoginRequest request = new UserLoginRequest();
        request.setEmail("test3@example.com");
        request.setPassword("Qwe12345!");

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("login-access");
        tokenResponse.setRefreshToken("login-refresh");
        tokenResponse.setExpiresIn(300);
        tokenResponse.setTokenType("Bearer");

        when(tokenService.login(any(UserLoginRequest.class)))
                .thenReturn(Mono.just(tokenResponse));

        webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.access_token").isEqualTo("login-access")
                .jsonPath("$.refresh_token").isEqualTo("login-refresh")
                .jsonPath("$.token_type").isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Refresh: /v1/auth/refresh-token возвращает новый токен")
    void refreshFlow_returnsNewTokenResponse() {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("ref-access");
        tokenResponse.setRefreshToken("ref-refresh");
        tokenResponse.setExpiresIn(300);
        tokenResponse.setTokenType("Bearer");

        when(tokenService.refresh(any()))
                .thenReturn(Mono.just(tokenResponse));

        String body = """
                {
                  "refresh_token": "some-refresh-token"
                }
                """;

        webTestClient.post()
                .uri("/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.access_token").isEqualTo("ref-access")
                .jsonPath("$.refresh_token").isEqualTo("ref-refresh")
                .jsonPath("$.token_type").isEqualTo("Bearer");
    }

    @Test
    @DisplayName("ME: /v1/auth/me возвращает информацию о текущем пользователе")
    void meFlow_returnsCurrentUserInfo() {
        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setId("1eaf40a8-d74f-4db2-aac3-f04fde97c29e");
        userInfo.setEmail("test3@example.com");
        userInfo.setRoles(List.of("offline_access", "default-roles-individuals", "uma_authorization"));
        userInfo.setCreatedAt(OffsetDateTime.parse("2025-11-29T00:00:00Z"));

        when(userService.getCurrentUser(any(Jwt.class)))
                .thenReturn(Mono.just(userInfo));

        webTestClient
                .mutateWith(mockJwt())
                .get()
                .uri("/v1/auth/me")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo("1eaf40a8-d74f-4db2-aac3-f04fde97c29e")
                .jsonPath("$.email").isEqualTo("test3@example.com")
                .jsonPath("$.roles[0]").isEqualTo("offline_access");
    }
}
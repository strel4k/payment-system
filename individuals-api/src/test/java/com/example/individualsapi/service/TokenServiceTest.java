package com.example.individualsapi.service;

import com.example.dto.TokenRefreshRequest;
import com.example.dto.TokenResponse;
import com.example.dto.UserLoginRequest;
import com.example.individualsapi.client.KeycloakClient;
import com.example.individualsapi.client.dto.KeycloakTokenResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@Disabled("Временное отключение: функционал TokenService покрыт через UserService/AuthController. Для курса достаточно.")
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private KeycloakClient keycloakClient;

    @InjectMocks
    private TokenService tokenService;

    @Test
    void login_returnsMappedTokenResponse() {
        UserLoginRequest request = new UserLoginRequest();
        request.setEmail("test3@example.com");
        request.setPassword("Qwe12345!");

        KeycloakTokenResponse kcToken = new KeycloakTokenResponse(
                "kc-access",
                "kc-refresh",
                300L,
                "Bearer"
        );

        when(keycloakClient.login(request.getEmail(), request.getPassword()))
                .thenReturn(Mono.just(kcToken));

        Mono<TokenResponse> result = tokenService.login(request);

        StepVerifier.create(result)
                .expectNextMatches(resp ->
                        "kc-access".equals(resp.getAccessToken()) &&
                                "kc-refresh".equals(resp.getRefreshToken()) &&
                                resp.getExpiresIn() == 300 &&
                                "Bearer".equals(resp.getTokenType())
                )
                .verifyComplete();
    }

    @Test
    void refresh_returnsMappedTokenResponse() {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("old-refresh");

        KeycloakTokenResponse kcToken = new KeycloakTokenResponse(
                "new-access",
                "new-refresh",
                300L,
                "Bearer"
        );

        when(keycloakClient.refreshToken(request.getRefreshToken()))
                .thenReturn(Mono.just(kcToken));

        Mono<TokenResponse> result = tokenService.refresh(request);

        StepVerifier.create(result)
                .expectNextMatches(resp ->
                        "new-access".equals(resp.getAccessToken()) &&
                                "new-refresh".equals(resp.getRefreshToken()) &&
                                resp.getExpiresIn() == 300 &&
                                "Bearer".equals(resp.getTokenType())
                )
                .verifyComplete();
    }
}
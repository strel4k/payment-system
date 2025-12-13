package com.example.individualsapi.service;

import com.example.dto.TokenResponse;
import com.example.dto.UserRegistrationRequest;
import com.example.individualsapi.client.KeycloakClient;
import com.example.individualsapi.client.dto.KeycloakTokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private KeycloakClient keycloakClient;

    @InjectMocks
    private UserService userService;

    @Test
    void register_returnsTokenResponseOnSuccess() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test4@example.com");
        request.setPassword("Qwe12345!");
        request.setConfirmPassword("Qwe12345!");

        KeycloakTokenResponse kcToken = new KeycloakTokenResponse(
                "access-123",
                "refresh-123",
                300L,
                "Bearer"
        );

        when(keycloakClient.createUser(request.getEmail(), request.getPassword()))
                .thenReturn(Mono.empty());
        when(keycloakClient.login(request.getEmail(), request.getPassword()))
                .thenReturn(Mono.just(kcToken));

        Mono<TokenResponse> result = userService.register(request);

        StepVerifier.create(result)
                .expectNextMatches(resp ->
                        "access-123".equals(resp.getAccessToken()) &&
                                "refresh-123".equals(resp.getRefreshToken()) &&
                                resp.getExpiresIn() == 300 &&
                                "Bearer".equals(resp.getTokenType())
                )
                .verifyComplete();
    }

    @Test
    void register_throwsErrorWhenPasswordsDoNotMatch() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test4@example.com");
        request.setPassword("Qwe12345!");
        request.setConfirmPassword("Qwe12345"); // без '!'

        Mono<TokenResponse> result = userService.register(request);

        StepVerifier.create(result)
                .expectErrorMatches(ex ->
                        ex instanceof IllegalArgumentException &&
                                ex.getMessage().contains("Passwords do not match")
                )
                .verify();
    }
}
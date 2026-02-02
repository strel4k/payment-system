package com.example.individualsapi.service;

import com.example.dto.TokenResponse;
import com.example.dto.UserRegistrationRequest;
import com.example.dto.person.PersonResponse;
import com.example.individualsapi.client.KeycloakClient;
import com.example.individualsapi.client.PersonServiceClient;
import com.example.individualsapi.client.dto.KeycloakTokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private KeycloakClient keycloakClient;

    @Mock
    private PersonServiceClient personServiceClient;

    @InjectMocks
    private UserService userService;

    // ------------ helpers ------------

    private static final UUID PERSON_USER_ID = UUID.randomUUID();

    private UserRegistrationRequest buildRequest(String email) {
        var req = new UserRegistrationRequest();
        req.setEmail(email);
        req.setPassword("Qwe12345!");
        req.setConfirmPassword("Qwe12345!");
        req.setFirstName("Ivan");
        req.setLastName("Petrov");
        return req;
    }

    private PersonResponse stubPersonResponse() {
        return new PersonResponse()
                .userId(PERSON_USER_ID)
                .email("test@example.com");
    }

    private KeycloakTokenResponse stubKcToken() {
        return new KeycloakTokenResponse("access-123", "refresh-123", 300L, "Bearer");
    }

    // ------------ 1. happy path: person-service → keycloak (с user_uid) → login → tokens ------------

    @Test
    void register_happyPath_callsPersonService_thenKeycloak_thenLogin() {
        var request = buildRequest("test@example.com");

        when(personServiceClient.createPerson(any()))
                .thenReturn(Mono.just(stubPersonResponse()));

        when(keycloakClient.createUserWithAttribute(
                eq("test@example.com"),
                eq("Qwe12345!"),
                eq(PERSON_USER_ID.toString())
        )).thenReturn(Mono.empty());

        when(keycloakClient.login("test@example.com", "Qwe12345!"))
                .thenReturn(Mono.just(stubKcToken()));

        StepVerifier.create(userService.register(request))
                .expectNextMatches(resp ->
                        "access-123".equals(resp.getAccessToken())
                                && "refresh-123".equals(resp.getRefreshToken())
                                && resp.getExpiresIn() == 300
                                && "Bearer".equals(resp.getTokenType())
                )
                .verifyComplete();

        // Проверка что все три шага вызваны
        verify(personServiceClient).createPerson(any());
        verify(keycloakClient).createUserWithAttribute(
                "test@example.com", "Qwe12345!", PERSON_USER_ID.toString()
        );
        verify(keycloakClient).login("test@example.com", "Qwe12345!");
    }

    // ------------ 2. пароли не совпадают → error до вызова person-service ------------

    @Test
    void register_passwordsMismatch_errorBeforeAnyCall() {
        var request = new UserRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("Qwe12345!");
        request.setConfirmPassword("OtherPass!");  // не совпадает

        StepVerifier.create(userService.register(request))
                .expectErrorMatches(ex ->
                        ex instanceof IllegalArgumentException
                                && ex.getMessage().contains("Passwords do not match")
                )
                .verify();

        // ничего не вызвало
        verify(personServiceClient, org.mockito.Mockito.never()).createPerson(any());
        verify(keycloakClient, org.mockito.Mockito.never()).createUserWithAttribute(any(), any(), any());
    }

    // ------------ 3. person-service падает → ошибка propagates, keycloak НЕ вызван ------------

    @Test
    void register_personServiceFails_keycloakNotCalled() {
        var request = buildRequest("fail@example.com");

        when(personServiceClient.createPerson(any()))
                .thenReturn(Mono.error(new RuntimeException("person-service unavailable")));

        StepVerifier.create(userService.register(request))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException
                                && ex.getMessage().contains("person-service unavailable")
                )
                .verify();

        verify(keycloakClient, org.mockito.Mockito.never()).createUserWithAttribute(any(), any(), any());
        verify(keycloakClient, org.mockito.Mockito.never()).login(any(), any());
    }

    // ------------ 4. keycloak создание user падает → ошибка propagates ------------

    @Test
    void register_keycloakCreateFails_errorPropagates() {
        var request = buildRequest("kcfail@example.com");

        when(personServiceClient.createPerson(any()))
                .thenReturn(Mono.just(stubPersonResponse()));

        when(keycloakClient.createUserWithAttribute(any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("keycloak 500")));

        when(keycloakClient.login(any(), any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(userService.register(request))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException
                                && ex.getMessage().contains("keycloak 500")
                )
                .verify();
    }

    // ------------ 5. login падает → error propagates ------------

    @Test
    void register_loginFails_errorPropagates() {
        var request = buildRequest("loginfail@example.com");

        when(personServiceClient.createPerson(any()))
                .thenReturn(Mono.just(stubPersonResponse()));

        when(keycloakClient.createUserWithAttribute(any(), any(), any()))
                .thenReturn(Mono.empty());

        when(keycloakClient.login("loginfail@example.com", "Qwe12345!"))
                .thenReturn(Mono.error(new RuntimeException("invalid credentials")));

        StepVerifier.create(userService.register(request))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException
                                && ex.getMessage().contains("invalid credentials")
                )
                .verify();
    }
}
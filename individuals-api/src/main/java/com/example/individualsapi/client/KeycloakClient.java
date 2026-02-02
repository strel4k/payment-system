package com.example.individualsapi.client;

import com.example.individualsapi.client.dto.KeycloakCreateUserRequest;
import com.example.individualsapi.client.dto.KeycloakTokenResponse;
import com.example.individualsapi.client.dto.KeycloakUserInfoResponse;
import com.example.individualsapi.security.AdminTokenStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakClient {

    private final WebClient keycloakWebClient;
    private final KeycloakProperties props;
    private final AdminTokenStorage adminTokenStorage;

    public Mono<Void> createUser(String email, String password) {
        String url = props.getBaseUrl() + "/admin/realms/" + props.getRealm() + "/users";
        KeycloakCreateUserRequest request = KeycloakCreateUserRequest.of(email, password);

        return adminTokenStorage.getValidToken()
                .flatMap(token -> keycloakWebClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(h -> h.setBearerAuth(token))
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(Void.class)
                );
    }


    public Mono<Void> createUserWithAttribute(String email, String password, String userUid) {
        String url = props.getBaseUrl() + "/admin/realms/" + props.getRealm() + "/users";
        KeycloakCreateUserRequest request = KeycloakCreateUserRequest.withUserUid(email, password, userUid);

        log.info("Creating Keycloak user for email: {} with user_uid: {}", email, userUid);

        return adminTokenStorage.getValidToken()
                .flatMap(token -> keycloakWebClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(h -> h.setBearerAuth(token))
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .doOnSuccess(v -> log.info("Keycloak user created successfully with user_uid: {}", userUid))
                );
    }

    public Mono<KeycloakTokenResponse> login(String email, String password) {
        String url = props.getBaseUrl() + "/realms/" + props.getRealm() + "/protocol/openid-connect/token";

        return keycloakWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", props.getClientId())
                        .with("client_secret", props.getClientSecret())
                        .with("username", email)
                        .with("password", password)
                )
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .doOnSuccess(t -> log.info("Token generated for {}", email));
    }

    public Mono<KeycloakTokenResponse> refreshToken(String refreshToken) {
        String url = props.getBaseUrl() + "/realms/" + props.getRealm() + "/protocol/openid-connect/token";

        return keycloakWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("client_id", props.getClientId())
                        .with("client_secret", props.getClientSecret())
                        .with("refresh_token", refreshToken)
                )
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .doOnSuccess(t -> log.info("Token refreshed"));
    }

    public Mono<KeycloakUserInfoResponse> getUserInfo(String accessToken) {
        String url = props.getBaseUrl() + "/realms/" + props.getRealm() + "/protocol/openid-connect/userinfo";

        return keycloakWebClient.get()
                .uri(url)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(KeycloakUserInfoResponse.class)
                .doOnSuccess(u -> log.info("Fetched user info: {}", u.email()));
    }
}
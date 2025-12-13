package com.example.individualsapi.client;

import com.example.individualsapi.client.dto.KeycloakCreateUserRequest;
import com.example.individualsapi.client.dto.KeycloakTokenResponse;
import com.example.individualsapi.client.dto.KeycloakUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakClient {

    private final WebClient keycloakWebClient;
    private final KeycloakProperties props;

    private final String realm = "individuals";
    private final String clientId = "individuals-client";
    private final String clientSecret = "secret";

    private Mono<String> getAdminAccessToken() {
        String url = props.getBaseUrl() + "/realms/master/protocol/openid-connect/token";

        return keycloakWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", "admin-cli")
                        .with("username", props.getAdmin().getUsername())
                        .with("password", props.getAdmin().getPassword())
                )
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(body -> (String) body.get("access_token"))
                .doOnNext(token -> log.info("Obtained admin access token from Keycloak"));
    }

    public Mono<Void> createUser(String email, String password) {

        String url = props.getBaseUrl() + "/admin/realms/" + props.getRealm() + "/users";

        KeycloakCreateUserRequest request = KeycloakCreateUserRequest.of(email, password);

        return getAdminAccessToken()
                .flatMap(token ->
                        keycloakWebClient.post()
                                .uri(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .headers(h -> h.setBearerAuth(token))
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(Void.class)
                )
                .doOnSuccess(v -> log.info("User created in Keycloak: email={}", email));
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
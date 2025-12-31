package com.example.individualsapi.security;

import com.example.individualsapi.client.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class InMemoryAdminTokenStorage implements AdminTokenStorage {

    private final WebClient keycloakWebClient;
    private final KeycloakProperties props;

    private final AtomicReference<CachedToken> cached = new AtomicReference<>();
    private final AtomicReference<Mono<CachedToken>> inFlight = new AtomicReference<>();

    @Override
    public Mono<String> getValidToken() {
        CachedToken current = cached.get();
        if (current != null && current.isValid()) {
            return Mono.just(current.token());
        }

        Mono<CachedToken> existing = inFlight.get();
        if (existing != null) {
            return existing.map(CachedToken::token);
        }

        Mono<CachedToken> fresh = fetchAdminToken()
                .doOnNext(t -> {
                    cached.set(t);
                    log.info("Admin token cached, expiresAt={}", t.expiresAt());
                })
                .doFinally(sig -> inFlight.set(null))
                .cache();

        if (inFlight.compareAndSet(null, fresh)) {
            return fresh.map(CachedToken::token);
        }
        return inFlight.get().map(CachedToken::token);
    }

    private Mono<CachedToken> fetchAdminToken() {
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
                .map(body -> {
                    String token = (String) body.get("access_token");
                    Number expiresIn = (Number) body.getOrDefault("expires_in", 60);
                    Instant expiresAt = Instant.now().plusSeconds(expiresIn.longValue() - 10);
                    return new CachedToken(token, expiresAt);
                });
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return token != null && expiresAt != null && Instant.now().isBefore(expiresAt);
        }
    }
}
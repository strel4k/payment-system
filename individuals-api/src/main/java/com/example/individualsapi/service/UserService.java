package com.example.individualsapi.service;

import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.UserRegistrationRequest;
import com.example.individualsapi.client.KeycloakClient;
import com.example.individualsapi.client.dto.KeycloakTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final KeycloakClient keycloakClient;

    public Mono<TokenResponse> register(UserRegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Mono.error(new IllegalArgumentException("Passwords do not match"));
        }

        String email = request.getEmail();
        String password = request.getPassword();

        return keycloakClient.createUser(email, password)
                .then(keycloakClient.login(email, password))
                .map(this::mapToTokenResponse);
    }

    public Mono<UserInfoResponse> getCurrentUser(Jwt jwt) {
        return Mono.just(mapFromJwt(jwt));
    }

    private TokenResponse mapToTokenResponse(KeycloakTokenResponse kcToken) {
        TokenResponse response = new TokenResponse();
        response.setAccessToken(kcToken.accessToken());
        response.setRefreshToken(kcToken.refreshToken());
        response.setExpiresIn((int) kcToken.expiresIn());
        response.setTokenType(kcToken.tokenType());
        return response;
    }

    private UserInfoResponse mapFromJwt(Jwt jwt) {
        UserInfoResponse response = new UserInfoResponse();

        response.setId(jwt.getSubject());

        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }
        response.setEmail(email);

        List<String> roles = Collections.emptyList();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List<?> list) {
                roles = list.stream()
                        .map(Object::toString)
                        .toList();
            }
        }
        response.setRoles(roles);

        Instant issuedAt = jwt.getIssuedAt();
        response.setCreatedAt(
                issuedAt != null ? OffsetDateTime.ofInstant(issuedAt, ZoneOffset.UTC) : null
        );

        return response;
    }
}
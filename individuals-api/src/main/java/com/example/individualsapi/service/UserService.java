package com.example.individualsapi.service;

import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.UserRegistrationRequest;
import com.example.dto.person.CreatePersonRequest;
import com.example.individualsapi.client.KeycloakClient;
import com.example.individualsapi.client.PersonServiceClient;
import com.example.individualsapi.client.dto.KeycloakTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final KeycloakClient keycloakClient;
    private final PersonServiceClient personServiceClient;

    public Mono<TokenResponse> register(UserRegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Mono.error(new IllegalArgumentException("Passwords do not match"));
        }

        String email = request.getEmail();
        String password = request.getPassword();

        CreatePersonRequest personRequest = new CreatePersonRequest()
                .email(email)
                .firstName(request.getFirstName() != null ? request.getFirstName() : "")
                .lastName(request.getLastName() != null ? request.getLastName() : "");

        log.info("Starting user registration for email: {}", email);

        return personServiceClient.createPerson(personRequest)
                .flatMap(personResponse -> {
                    String userId = personResponse.getUserId().toString();
                    log.info("Person created with userId: {}", userId);

                    return keycloakClient.createUserWithAttribute(email, password, userId)
                            .doOnSuccess(v -> log.info("User created in Keycloak with user_uid: {}", userId))
                            .onErrorResume(keycloakError -> {
                                log.error("Keycloak user creation failed for email: {}, rolling back person creation", email, keycloakError);

                                RuntimeException registrationError = new RuntimeException(
                                        "Registration failed: " + keycloakError.getMessage(),
                                        keycloakError
                                );

                                return personServiceClient.deletePerson(personResponse.getUserId())
                                        .doOnSuccess(v -> log.info("Successfully rolled back person with userId: {}", userId))
                                        .doOnError(deleteError -> log.error("CRITICAL: Failed to rollback person with userId: {}. Manual cleanup required!", userId, deleteError))
                                        .thenReturn(true)
                                        .onErrorReturn(false)
                                        .<Void>flatMap(deleteSucceeded -> Mono.error(registrationError));  // Cast to Mono<Void>
                            })
                            .then(keycloakClient.login(email, password))
                            .map(this::mapToTokenResponse)
                            .doOnSuccess(tokens -> log.info("Registration completed successfully for email: {}", email));
                })
                .doOnError(error -> log.error("Registration failed for email: {}", email, error));
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
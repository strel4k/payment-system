package com.example.individualsapi.service;

import com.example.dto.TokenRefreshRequest;
import com.example.dto.TokenResponse;
import com.example.dto.UserLoginRequest;
import com.example.individualsapi.client.KeycloakClient;
import com.example.individualsapi.client.dto.KeycloakTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final KeycloakClient keycloakClient;

    public Mono<TokenResponse> login(UserLoginRequest request) {
        return keycloakClient.login(request.getEmail(), request.getPassword())
                .map(this::mapToTokenResponse);
    }

    public Mono<TokenResponse> refresh(TokenRefreshRequest request) {
        return keycloakClient.refreshToken(request.getRefreshToken())
                .map(this::mapToTokenResponse);
    }

    private TokenResponse mapToTokenResponse(KeycloakTokenResponse kcToken) {
        TokenResponse response = new TokenResponse();
        response.setAccessToken(kcToken.accessToken());
        response.setRefreshToken(kcToken.refreshToken());
        response.setExpiresIn((int) kcToken.expiresIn());
        response.setTokenType(kcToken.tokenType());
        return response;
    }
}
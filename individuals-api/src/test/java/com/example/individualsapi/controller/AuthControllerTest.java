package com.example.individualsapi.controller;

import com.example.dto.TokenRefreshRequest;
import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.UserLoginRequest;
import com.example.dto.UserRegistrationRequest;
import com.example.individualsapi.service.TokenService;
import com.example.individualsapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthController authController;

    @Test
    void me_returnsCurrentUserInfo() {
        String tokenValue = "dummy-access";

        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .claim("sub", "1eaf40a8-d74f-4db2-aac3-f04fde97c29e")
                .claim("email", "test3@example.com")
                .build();

        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setId("1eaf40a8-d74f-4db2-aac3-f04fde97c29e");
        userInfo.setEmail("test3@example.com");
        userInfo.setRoles(List.of("offline_access", "default-roles-individuals", "uma_authorization"));
        userInfo.setCreatedAt(OffsetDateTime.parse("2025-11-29T00:00:00Z"));

        when(userService.getCurrentUser(jwt))
                .thenReturn(Mono.just(userInfo));

        Mono<UserInfoResponse> result = authController.me(jwt);

        StepVerifier.create(result)
                .expectNextMatches(resp ->
                        resp.getId().equals(userInfo.getId()) &&
                                resp.getEmail().equals(userInfo.getEmail()) &&
                                resp.getRoles().contains("offline_access")
                )
                .verifyComplete();
    }

    @Test
    void register_returnsTokenResponse() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test4@example.com");
        request.setPassword("Qwe12345!");
        request.setConfirmPassword("Qwe12345!");

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("access");
        tokenResponse.setRefreshToken("refresh");
        tokenResponse.setExpiresIn(300);
        tokenResponse.setTokenType("Bearer");

        when(userService.register(any(UserRegistrationRequest.class)))
                .thenReturn(Mono.just(tokenResponse));

        Mono<TokenResponse> result = authController.register(request);

        StepVerifier.create(result)
                .expectNextMatches(resp ->
                        "access".equals(resp.getAccessToken()) &&
                                "refresh".equals(resp.getRefreshToken()) &&
                                "Bearer".equals(resp.getTokenType())
                )
                .verifyComplete();
    }

    @Test
    void login_returnsTokenResponse() {
        UserLoginRequest request = new UserLoginRequest();
        request.setEmail("test3@example.com");
        request.setPassword("Qwe12345!");

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("access-login");
        tokenResponse.setRefreshToken("refresh-login");
        tokenResponse.setExpiresIn(300);
        tokenResponse.setTokenType("Bearer");

        when(tokenService.login(any(UserLoginRequest.class)))
                .thenReturn(Mono.just(tokenResponse));

        Mono<TokenResponse> result = authController.login(request);

        StepVerifier.create(result)
                .expectNextMatches(resp ->
                        "access-login".equals(resp.getAccessToken()) &&
                                "refresh-login".equals(resp.getRefreshToken())
                )
                .verifyComplete();
    }

    @Test
    void refreshToken_returnsNewTokenResponse() {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("some-refresh-token");

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("access-refreshed");
        tokenResponse.setRefreshToken("refresh-refreshed");
        tokenResponse.setExpiresIn(300);
        tokenResponse.setTokenType("Bearer");

        when(tokenService.refresh(any(TokenRefreshRequest.class)))
                .thenReturn(Mono.just(tokenResponse));

        Mono<TokenResponse> result = authController.refreshToken(request);

        StepVerifier.create(result)
                .expectNextMatches(resp ->
                        "access-refreshed".equals(resp.getAccessToken()) &&
                                "refresh-refreshed".equals(resp.getRefreshToken())
                )
                .verifyComplete();
    }
}
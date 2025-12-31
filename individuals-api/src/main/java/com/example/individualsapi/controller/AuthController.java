package com.example.individualsapi.controller;

import com.example.dto.TokenRefreshRequest;
import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.UserLoginRequest;
import com.example.dto.UserRegistrationRequest;
import com.example.individualsapi.service.TokenService;
import com.example.individualsapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;

    @PostMapping(value = "/registration", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TokenResponse> register(@RequestBody UserRegistrationRequest request) {
        return userService.register(request);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<TokenResponse> login(@RequestBody UserLoginRequest request) {
        return tokenService.login(request);
    }

    @PostMapping(value = "/refresh-token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<TokenResponse> refreshToken(@RequestBody TokenRefreshRequest request) {
        return tokenService.refresh(request);
    }

    @GetMapping("/me")
    public Mono<UserInfoResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getCurrentUser(jwt);
    }
}
package com.example.individualsapi.security;

import reactor.core.publisher.Mono;

public interface AdminTokenStorage {
    Mono<String> getValidToken();
}
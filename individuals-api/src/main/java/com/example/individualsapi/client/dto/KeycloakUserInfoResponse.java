package com.example.individualsapi.client.dto;

import java.util.List;

public record KeycloakUserInfoResponse(
        String sub,
        String email,
        List<String> roles
) {
}

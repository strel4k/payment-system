package com.example.individualsapi.client.dto;

import java.util.List;

public record KeycloakCreateUserRequest(
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        boolean emailVerified,
        List<Credential> credentials,
        List<String> requiredActions
) {

    public record Credential(String type, String value, boolean temporary) {}

    public static KeycloakCreateUserRequest of(String email, String password) {
        String localPart = email;
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            localPart = email.substring(0, atIndex);
        }

        return new KeycloakCreateUserRequest(
                email,
                email,
                localPart,
                "User",
                true,
                true,
                List.of(new Credential("password", password, false)),
                List.of()
        );
    }
}

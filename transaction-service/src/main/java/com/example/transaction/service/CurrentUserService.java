package com.example.transaction.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CurrentUserService {

    public Optional<UUID> getCurrentUserUid() {
        return getJwt()
                .map(jwt -> jwt.getSubject())
                .map(UUID::fromString);
    }

    public UUID requireCurrentUserUid() {
        return getCurrentUserUid()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
    }

    public Optional<String> getCurrentUserEmail() {
        return getJwt()
                .map(jwt -> jwt.getClaimAsString("email"));
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    public boolean isOwner(UUID resourceOwnerUid) {
        return getCurrentUserUid()
                .map(uid -> uid.equals(resourceOwnerUid))
                .orElse(false);
    }

    public boolean canAccess(UUID resourceOwnerUid) {
        return isOwner(resourceOwnerUid) || hasRole("admin");
    }

    private Optional<Jwt> getJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt)) {
            return Optional.empty();
        }
        return Optional.of((Jwt) auth.getPrincipal());
    }
}
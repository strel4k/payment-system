package com.example.transaction.service;

import com.example.transaction.config.AppProperties;
import com.example.transaction.exception.InvalidTransactionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitRequestCache {

    private final AppProperties appProperties;
    private final Map<UUID, InitRequest> cache = new ConcurrentHashMap<>();

    public void put(InitRequest request) {
        cache.put(request.getRequestUid(), request);
        log.debug("Stored init request: {}, expires at: {}",
                request.getRequestUid(), request.getExpiresAt());
    }


    public Optional<InitRequest> get(UUID requestUid) {
        InitRequest request = cache.get(requestUid);

        if (request == null) {
            return Optional.empty();
        }

        if (request.isExpired()) {
            cache.remove(requestUid);
            log.debug("Init request expired and removed: {}", requestUid);
            return Optional.empty();
        }

        return Optional.of(request);
    }

    public InitRequest getAndRemove(UUID requestUid) {
        InitRequest request = cache.remove(requestUid);

        if (request == null) {
            throw new InvalidTransactionException(
                    "Init request not found: " + requestUid);
        }

        if (request.isExpired()) {
            throw new InvalidTransactionException(
                    "Init request expired: " + requestUid);
        }

        log.debug("Retrieved and removed init request: {}", requestUid);
        return request;
    }

    public void remove(UUID requestUid) {
        cache.remove(requestUid);
        log.debug("Removed init request: {}", requestUid);
    }

    public boolean exists(UUID requestUid) {
        return get(requestUid).isPresent();
    }

    public LocalDateTime calculateExpiresAt() {
        return LocalDateTime.now().plusMinutes(appProperties.getInitRequestTtlMinutes());
    }

    public int size() {
        return cache.size();
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupExpired() {
        int before = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removed = before - cache.size();

        if (removed > 0) {
            log.info("Cleaned up {} expired init requests", removed);
        }
    }
}
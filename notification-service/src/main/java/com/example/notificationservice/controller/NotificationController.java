package com.example.notificationservice.controller;

import com.example.notificationservice.controller.dto.NotificationResponse;
import com.example.notificationservice.controller.dto.NotificationStatusRequest;
import com.example.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{userUid}")
    public ResponseEntity<List<NotificationResponse>> getByUserUid(@PathVariable UUID userUid) {
        log.info("GET /api/v1/notifications/{}", userUid);
        return ResponseEntity.ok(notificationService.getByUserUid(userUid));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<NotificationResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationStatusRequest request) {
        log.info("PATCH /api/v1/notifications/{}/status status={}", id, request.status());
        return ResponseEntity.ok(notificationService.updateStatus(id, request.status()));
    }
}
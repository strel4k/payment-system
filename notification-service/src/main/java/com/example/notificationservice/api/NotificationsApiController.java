package com.example.notificationservice.api;

import com.example.dto.notification.NotificationResponse;
import com.example.dto.notification.NotificationStatusRequest;
import com.example.notificationservice.entity.NotificationStatus;
import com.example.notificationservice.mapper.NotificationMapper;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationsApiController implements NotificationsApi {

    private final NotificationService notificationService;

    @Override
    public ResponseEntity<List<NotificationResponse>> getNotificationsByUserUid(UUID userUid) {
        log.info("GET /api/v1/notifications/{}", userUid);
        return ResponseEntity.ok(notificationService.getByUserUid(userUid));
    }

    @Override
    public ResponseEntity<NotificationResponse> updateNotificationStatus(
            UUID id,
            NotificationStatusRequest request) {
        log.info("PATCH /api/v1/notifications/{}/status status={}", id, request.getStatus());
        NotificationStatus status = NotificationStatus.valueOf(request.getStatus().getValue());
        return ResponseEntity.ok(notificationService.updateStatus(id, status));
    }
}
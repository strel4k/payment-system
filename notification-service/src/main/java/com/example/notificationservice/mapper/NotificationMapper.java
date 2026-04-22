package com.example.notificationservice.mapper;

import com.example.kafka.NotificationCreated;
import com.example.notificationservice.controller.dto.NotificationResponse;
import com.example.notificationservice.entity.Notification;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationMapper {

    public Notification toEntity(NotificationCreated event) {
        return Notification.builder()
                .userUid(UUID.fromString(event.getUserUid().toString()))
                .message(event.getMessage().toString())
                .subject(event.getSubject().toString())
                .createdBy(event.getCreatedBy().toString())
                .recipientEmail(event.getRecipientEmail() != null
                        ? event.getRecipientEmail().toString()
                        : null)
                .build();
    }

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getUid(),
                notification.getUserUid(),
                notification.getMessage(),
                notification.getSubject(),
                notification.getCreatedBy(),
                notification.getRecipientEmail(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getModifiedAt()
        );
    }
}
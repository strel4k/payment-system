package com.example.notificationservice.mapper;

import com.example.dto.notification.NotificationResponse;
import com.example.dto.notification.NotificationStatus;
import com.example.kafka.NotificationCreated;
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
        NotificationResponse response = new NotificationResponse();
        response.setUid(notification.getUid());
        response.setUserUid(notification.getUserUid());
        response.setMessage(notification.getMessage());
        response.setSubject(notification.getSubject());
        response.setCreatedBy(notification.getCreatedBy());
        response.setRecipientEmail(notification.getRecipientEmail());
        response.setStatus(NotificationStatus.fromValue(notification.getStatus().name()));
        response.setCreatedAt(notification.getCreatedAt());
        response.setModifiedAt(notification.getModifiedAt());
        return response;
    }
}
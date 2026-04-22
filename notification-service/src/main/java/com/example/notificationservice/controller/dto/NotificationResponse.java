package com.example.notificationservice.controller.dto;

import com.example.notificationservice.entity.NotificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID uid,
        UUID userUid,
        String message,
        String subject,
        String createdBy,
        String recipientEmail,
        NotificationStatus status,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
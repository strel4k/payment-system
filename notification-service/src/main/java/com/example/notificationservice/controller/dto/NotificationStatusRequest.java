package com.example.notificationservice.controller.dto;

import com.example.notificationservice.entity.NotificationStatus;
import jakarta.validation.constraints.NotNull;

public record NotificationStatusRequest(
        @NotNull NotificationStatus status
) {}
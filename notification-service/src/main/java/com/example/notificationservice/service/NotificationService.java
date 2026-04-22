package com.example.notificationservice.service;

import com.example.notificationservice.controller.dto.NotificationResponse;
import com.example.notificationservice.email.EmailService;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.NotificationStatus;
import com.example.notificationservice.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String REGISTRATION_SUBJECT = "REGISTRATION";

    private final NotificationPersistenceService persistenceService;
    private final EmailService emailService;
    private final NotificationMapper notificationMapper;

    public void processNotification(Notification notification) {
        log.info("Processing notification: userUid={} subject={}",
                notification.getUserUid(), notification.getSubject());

        Notification saved = persistenceService.save(notification);

        if (REGISTRATION_SUBJECT.equalsIgnoreCase(saved.getSubject())
                && saved.getRecipientEmail() != null
                && !saved.getRecipientEmail().isBlank()) {
            emailService.sendEmail(
                    saved.getRecipientEmail(),
                    saved.getSubject(),
                    saved.getMessage()
            );
        }
    }

    public List<NotificationResponse> getByUserUid(UUID userUid) {
        return persistenceService.findByUserUid(userUid)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    public NotificationResponse updateStatus(UUID id, NotificationStatus status) {
        Notification updated = persistenceService.updateStatus(id, status);
        return notificationMapper.toResponse(updated);
    }
}
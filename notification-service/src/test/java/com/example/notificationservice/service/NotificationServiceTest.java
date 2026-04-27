package com.example.notificationservice.service;

import com.example.dto.notification.NotificationResponse;
import com.example.notificationservice.email.EmailService;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.NotificationStatus;
import com.example.notificationservice.mapper.NotificationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationPersistenceService persistenceService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private static final UUID USER_UID  = UUID.randomUUID();
    private static final UUID NOTIF_UID = UUID.randomUUID();

    @Test
    @DisplayName("processNotification — REGISTRATION + email → сохраняет и отправляет email")
    void processNotification_registrationWithEmail_savesAndSendsEmail() {
        Notification input = buildNotification("REGISTRATION", "user@test.com");
        Notification saved  = buildNotification("REGISTRATION", "user@test.com");
        when(persistenceService.save(input)).thenReturn(saved);

        notificationService.processNotification(input);

        verify(persistenceService).save(input);
        verify(emailService).sendEmail("user@test.com", "REGISTRATION", "Welcome!");
    }

    @Test
    @DisplayName("processNotification — REGISTRATION без email → сохраняет, email не отправляет")
    void processNotification_registrationWithoutEmail_savesButNoEmail() {
        Notification input = buildNotification("REGISTRATION", null);
        Notification saved  = buildNotification("REGISTRATION", null);
        when(persistenceService.save(input)).thenReturn(saved);

        notificationService.processNotification(input);

        verify(persistenceService).save(input);
        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("processNotification — не REGISTRATION → сохраняет, email не отправляет")
    void processNotification_nonRegistration_savesButNoEmail() {
        Notification input = buildNotification("PAYMENT_COMPLETED", "user@test.com");
        Notification saved  = buildNotification("PAYMENT_COMPLETED", "user@test.com");
        when(persistenceService.save(input)).thenReturn(saved);

        notificationService.processNotification(input);

        verify(persistenceService).save(input);
        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("getByUserUid — возвращает список NotificationResponse")
    void getByUserUid_returnsMappedResponses() {
        Notification notif = buildNotification("REGISTRATION", null);
        NotificationResponse response = buildResponse();

        when(persistenceService.findByUserUid(USER_UID)).thenReturn(List.of(notif));
        when(notificationMapper.toResponse(notif)).thenReturn(response);

        List<NotificationResponse> result = notificationService.getByUserUid(USER_UID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(response);
    }

    @Test
    @DisplayName("updateStatus — делегирует в persistenceService и маппит результат")
    void updateStatus_delegatesAndMaps() {
        Notification updated  = buildNotification("REGISTRATION", null);
        NotificationResponse response = buildResponse();

        when(persistenceService.updateStatus(NOTIF_UID, NotificationStatus.COMPLETED))
                .thenReturn(updated);
        when(notificationMapper.toResponse(updated)).thenReturn(response);

        NotificationResponse result = notificationService.updateStatus(NOTIF_UID, NotificationStatus.COMPLETED);

        assertThat(result).isEqualTo(response);
        verify(persistenceService).updateStatus(NOTIF_UID, NotificationStatus.COMPLETED);
    }

    private Notification buildNotification(String subject, String recipientEmail) {
        return Notification.builder()
                .userUid(USER_UID)
                .message("Welcome!")
                .subject(subject)
                .createdBy("individuals-api")
                .recipientEmail(recipientEmail)
                .build();
    }

    private NotificationResponse buildResponse() {
        NotificationResponse response = new NotificationResponse();
        response.setUid(NOTIF_UID);
        response.setUserUid(USER_UID);
        response.setMessage("Welcome!");
        response.setSubject("REGISTRATION");
        response.setStatus(com.example.dto.notification.NotificationStatus.NEW);
        return response;
    }
}
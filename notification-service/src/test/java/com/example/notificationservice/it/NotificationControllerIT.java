package com.example.notificationservice.it;

import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.NotificationStatus;
import com.example.notificationservice.it.config.AbstractIT;
import com.example.notificationservice.kafka.NotificationKafkaConsumer;
import com.example.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationControllerIT extends AbstractIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    private NotificationKafkaConsumer notificationKafkaConsumer;

    private static final UUID USER_UID = UUID.randomUUID();

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
    }

    // ── GET /api/v1/notifications/{userUid} ───────────────────────

    @Test
    @DisplayName("GET /notifications/{userUid} — возвращает уведомления пользователя")
    void getByUserUid_returnsNotifications() {
        saveNotification(USER_UID, "Welcome!", "REGISTRATION", "individuals-api");
        saveNotification(USER_UID, "Payment done", "PAYMENT_COMPLETED", "transaction-service");

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/v1/notifications/" + USER_UID, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("GET /notifications/{userUid} — пустой список если уведомлений нет")
    void getByUserUid_returnsEmptyList() {
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/v1/notifications/" + UUID.randomUUID(), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("GET /notifications/{userUid} — не возвращает уведомления другого пользователя")
    void getByUserUid_doesNotReturnOtherUsersNotifications() {
        saveNotification(USER_UID, "My notification", "REGISTRATION", "individuals-api");
        saveNotification(UUID.randomUUID(), "Other notification", "REGISTRATION", "individuals-api");

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/v1/notifications/" + USER_UID, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    // ── PATCH /api/v1/notifications/{id}/status ───────────────────

    @Test
    @DisplayName("PATCH /notifications/{id}/status — обновляет статус NEW → COMPLETED")
    void updateStatus_updatesFromNewToCompleted() {
        Notification saved = saveNotification(USER_UID, "Test", "REGISTRATION", "individuals-api");

        Map<String, String> request = Map.of("status", "COMPLETED");
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/notifications/" + saved.getUid() + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Notification updated = notificationRepository.findById(saved.getUid()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.COMPLETED);
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/status — 404 для несуществующего ID")
    void updateStatus_returns404ForUnknownId() {
        Map<String, String> request = Map.of("status", "COMPLETED");
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/notifications/" + UUID.randomUUID() + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    @DisplayName("GET /actuator/health — доступен без авторизации")
    void actuatorHealth_returns200() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
    }

    // ── helpers ───────────────────────────────────────────────────

    private Notification saveNotification(UUID userUid, String message,
                                          String subject, String createdBy) {
        Notification notification = Notification.builder()
                .userUid(userUid)
                .message(message)
                .subject(subject)
                .createdBy(createdBy)
                .build();
        return notificationRepository.save(notification);
    }
}
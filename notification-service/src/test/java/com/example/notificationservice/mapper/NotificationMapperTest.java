package com.example.notificationservice.mapper;

import com.example.dto.notification.NotificationResponse;
import com.example.dto.notification.NotificationStatus;
import com.example.kafka.NotificationCreated;
import com.example.notificationservice.entity.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {

    private final NotificationMapper mapper = new NotificationMapper();

    private static final UUID USER_UID = UUID.randomUUID();

    @Test
    @DisplayName("toEntity — корректно маппит все поля из Avro события")
    void toEntity_mapsAllFields() {
        NotificationCreated event = NotificationCreated.newBuilder()
                .setUserUid(USER_UID.toString())
                .setMessage("Welcome!")
                .setSubject("REGISTRATION")
                .setCreatedBy("individuals-api")
                .setRecipientEmail("user@test.com")
                .build();

        Notification entity = mapper.toEntity(event);

        assertThat(entity.getUserUid()).isEqualTo(USER_UID);
        assertThat(entity.getMessage()).isEqualTo("Welcome!");
        assertThat(entity.getSubject()).isEqualTo("REGISTRATION");
        assertThat(entity.getCreatedBy()).isEqualTo("individuals-api");
        assertThat(entity.getRecipientEmail()).isEqualTo("user@test.com");
        assertThat(entity.getStatus()).isEqualTo(com.example.notificationservice.entity.NotificationStatus.NEW);
    }

    @Test
    @DisplayName("toEntity — recipientEmail null если не передан")
    void toEntity_nullRecipientEmail() {
        NotificationCreated event = NotificationCreated.newBuilder()
                .setUserUid(USER_UID.toString())
                .setMessage("Payment done")
                .setSubject("PAYMENT_COMPLETED")
                .setCreatedBy("transaction-service")
                .setRecipientEmail(null)
                .build();

        Notification entity = mapper.toEntity(event);

        assertThat(entity.getRecipientEmail()).isNull();
    }

    @Test
    @DisplayName("toResponse — корректно маппит все поля entity в сгенерированный DTO")
    void toResponse_mapsAllFields() {
        UUID uid = UUID.randomUUID();
        Notification notification = Notification.builder()
                .userUid(USER_UID)
                .message("Welcome!")
                .subject("REGISTRATION")
                .createdBy("individuals-api")
                .recipientEmail("user@test.com")
                .build();
        notification.setUid(uid);

        NotificationResponse response = mapper.toResponse(notification);

        assertThat(response.getUid()).isEqualTo(uid);
        assertThat(response.getUserUid()).isEqualTo(USER_UID);
        assertThat(response.getMessage()).isEqualTo("Welcome!");
        assertThat(response.getSubject()).isEqualTo("REGISTRATION");
        assertThat(response.getCreatedBy()).isEqualTo("individuals-api");
        assertThat(response.getRecipientEmail()).isEqualTo("user@test.com");
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.NEW);
    }
}
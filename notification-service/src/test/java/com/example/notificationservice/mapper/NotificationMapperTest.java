package com.example.notificationservice.mapper;

import com.example.kafka.NotificationCreated;
import com.example.notificationservice.controller.dto.NotificationResponse;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.NotificationStatus;
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
        assertThat(entity.getStatus()).isEqualTo(NotificationStatus.NEW);
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
    @DisplayName("toResponse — корректно маппит все поля entity в DTO")
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

        assertThat(response.uid()).isEqualTo(uid);
        assertThat(response.userUid()).isEqualTo(USER_UID);
        assertThat(response.message()).isEqualTo("Welcome!");
        assertThat(response.subject()).isEqualTo("REGISTRATION");
        assertThat(response.createdBy()).isEqualTo("individuals-api");
        assertThat(response.recipientEmail()).isEqualTo("user@test.com");
        assertThat(response.status()).isEqualTo(NotificationStatus.NEW);
    }
}
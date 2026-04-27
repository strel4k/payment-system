package com.example.notificationservice.kafka;

import com.example.kafka.NotificationCreated;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.mapper.NotificationMapper;
import com.example.notificationservice.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationKafkaConsumer consumer;

    @Test
    @DisplayName("consume — маппит событие и передаёт в сервис")
    void consume_mapsEventAndCallsService() {
        NotificationCreated event = buildEvent("REGISTRATION", "user@test.com");
        Notification notification = Notification.builder()
                .userUid(UUID.randomUUID())
                .message("Welcome!")
                .subject("REGISTRATION")
                .createdBy("individuals-api")
                .build();

        when(notificationMapper.toEntity(event)).thenReturn(notification);

        consumer.consume(event);

        verify(notificationMapper).toEntity(event);
        verify(notificationService).processNotification(notification);
    }

    @Test
    @DisplayName("consume — пробрасывает исключение при ошибке обработки")
    void consume_rethrowsExceptionOnServiceFailure() {
        NotificationCreated event = buildEvent("PAYMENT_COMPLETED", null);
        Notification notification = Notification.builder()
                .userUid(UUID.randomUUID())
                .message("Done")
                .subject("PAYMENT_COMPLETED")
                .createdBy("transaction-service")
                .build();

        when(notificationMapper.toEntity(event)).thenReturn(notification);
        doThrow(new RuntimeException("DB error"))
                .when(notificationService).processNotification(any());

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB error");
    }

    private NotificationCreated buildEvent(String subject, String recipientEmail) {
        return NotificationCreated.newBuilder()
                .setUserUid(UUID.randomUUID().toString())
                .setMessage("Welcome!")
                .setSubject(subject)
                .setCreatedBy("individuals-api")
                .setRecipientEmail(recipientEmail)
                .build();
    }
}
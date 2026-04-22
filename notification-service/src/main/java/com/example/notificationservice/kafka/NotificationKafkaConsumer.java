package com.example.notificationservice.kafka;

import com.example.kafka.NotificationCreated;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.mapper.NotificationMapper;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @KafkaListener(
            topics = "${kafka.topics.notification-created}",
            containerFactory = "notificationListenerContainerFactory"
    )
    public void consume(NotificationCreated event) {
        log.info("Received NotificationCreated: userUid={} subject={} createdBy={}",
                event.getUserUid(), event.getSubject(), event.getCreatedBy());

        try {
            Notification notification = notificationMapper.toEntity(event);
            notificationService.processNotification(notification);
        } catch (Exception e) {
            log.error("Failed to process NotificationCreated: userUid={} subject={}",
                    event.getUserUid(), event.getSubject(), e);
            throw e;
        }
    }
}
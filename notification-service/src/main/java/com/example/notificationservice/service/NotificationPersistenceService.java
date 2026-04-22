package com.example.notificationservice.service;

import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.NotificationStatus;
import com.example.notificationservice.exception.NotificationNotFoundException;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPersistenceService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification save(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        log.debug("Notification saved: uid={} userUid={} subject={}",
                saved.getUid(), saved.getUserUid(), saved.getSubject());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Notification> findByUserUid(UUID userUid) {
        return notificationRepository.findByUserUidOrderByCreatedAtDesc(userUid);
    }

    @Transactional
    public Notification updateStatus(UUID id, NotificationStatus status) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));

        notification.setStatus(status);
        Notification updated = notificationRepository.save(notification);
        log.info("Notification status updated: uid={} status={}", id, status);
        return updated;
    }
}
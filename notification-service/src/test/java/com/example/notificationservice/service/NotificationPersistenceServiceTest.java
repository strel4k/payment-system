package com.example.notificationservice.service;

import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.NotificationStatus;
import com.example.notificationservice.exception.NotificationNotFoundException;
import com.example.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPersistenceServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationPersistenceService persistenceService;

    private static final UUID USER_UID  = UUID.randomUUID();
    private static final UUID NOTIF_UID = UUID.randomUUID();

    @Test
    @DisplayName("save — сохраняет уведомление через репозиторий")
    void save_delegatesToRepository() {
        Notification notification = buildNotification();
        when(notificationRepository.save(any())).thenReturn(notification);

        Notification result = persistenceService.save(notification);

        verify(notificationRepository).save(notification);
        assertThat(result).isEqualTo(notification);
    }

    @Test
    @DisplayName("findByUserUid — возвращает список уведомлений пользователя")
    void findByUserUid_returnsList() {
        Notification notification = buildNotification();
        when(notificationRepository.findByUserUidOrderByCreatedAtDesc(USER_UID))
                .thenReturn(List.of(notification));

        List<Notification> result = persistenceService.findByUserUid(USER_UID);

        assertThat(result).hasSize(1);
        verify(notificationRepository).findByUserUidOrderByCreatedAtDesc(USER_UID);
    }

    @Test
    @DisplayName("updateStatus — обновляет статус и сохраняет")
    void updateStatus_updatesAndSaves() {
        Notification notification = buildNotification();
        when(notificationRepository.findById(NOTIF_UID)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenReturn(notification);

        persistenceService.updateStatus(NOTIF_UID, NotificationStatus.COMPLETED);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.COMPLETED);
    }

    @Test
    @DisplayName("updateStatus — NotificationNotFoundException если ID не найден")
    void updateStatus_throwsIfNotFound() {
        when(notificationRepository.findById(NOTIF_UID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> persistenceService.updateStatus(NOTIF_UID, NotificationStatus.COMPLETED))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining(NOTIF_UID.toString());
    }

    private Notification buildNotification() {
        return Notification.builder()
                .userUid(USER_UID)
                .message("Welcome!")
                .subject("REGISTRATION")
                .createdBy("individuals-api")
                .build();
    }
}
package tn.esprit.gestioncours.Services;

import tn.esprit.gestioncours.DTO.NotificationResponseDto;
import tn.esprit.gestioncours.Entities.NotificationType;

import java.util.List;

public interface INotificationService {

    NotificationResponseDto createNotificationForUser(Long userId, String message, NotificationType type);

    List<NotificationResponseDto> getNotificationsForUser(Long userId);

    List<NotificationResponseDto> getUnreadNotificationsForUser(Long userId);

    NotificationResponseDto markAsRead(Long id);

    void deleteNotification(Long id);
}


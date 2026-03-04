package tn.esprit.gestioncours.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.gestioncours.DTO.NotificationResponseDto;
import tn.esprit.gestioncours.Entities.Notification;
import tn.esprit.gestioncours.Entities.NotificationType;
import tn.esprit.gestioncours.Repositories.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationResponseDto createNotificationForUser(Long userId, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        return mapToDto(saved);
    }

    @Override
    public List<NotificationResponseDto> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponseDto> getUnreadNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponseDto markAsRead(Long id) {
        return notificationRepository.findById(id)
                .map(notification -> {
                    notification.setRead(true);
                    Notification updated = notificationRepository.save(notification);
                    return mapToDto(updated);
                })
                .orElse(null);
    }

    @Override
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    private NotificationResponseDto mapToDto(Notification notification) {
        return new NotificationResponseDto(
                notification.getId(),
                notification.getUserId(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getType()
        );
    }
}


package esprit.inscription.service;

import esprit.inscription.dto.DashboardStatsDTO;
import esprit.inscription.dto.NotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RealTimeStatsService {

    private final SimpMessagingTemplate messagingTemplate;
    private final StatisticsService statisticsService;

    /**
     * Broadcasts updated dashboard statistics to all subscribers on "/topic/stats".
     */
    public void pushStatsUpdate() {
        DashboardStatsDTO stats = statisticsService.getDashboardStats();
        messagingTemplate.convertAndSend("/topic/stats", stats);
    }

    /**
     * Sends a real-time notification to all users.
     */
    public void broadcastNotification(String title, String message, NotificationDTO.Type type) {
        NotificationDTO notification = createNotification(title, message, type, null);
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    /**
     * Sends a real-time notification to admin topic only.
     */
    public void sendAdminNotification(String title, String message, NotificationDTO.Type type) {
        NotificationDTO notification = createNotification(title, message, type, null);
        messagingTemplate.convertAndSend("/topic/admin-notifications", notification);
    }

    private NotificationDTO createNotification(String title, String message, NotificationDTO.Type type, Long userId) {
        return NotificationDTO.builder()
                .id(UUID.randomUUID().toString())
                .title(title)
                .message(message)
                .type(type)
                .userId(userId)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    /**
     * Sends a private notification to a specific user.
     */
    public void sendPrivateNotification(Long userId, String title, String message, NotificationDTO.Type type) {
        NotificationDTO notification = NotificationDTO.builder()
                .id(UUID.randomUUID().toString())
                .title(title)
                .message(message)
                .type(type)
                .userId(userId)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        // STOMP private queues usually follow "/queue/notifications-{userId}" or
        // similar
        messagingTemplate.convertAndSend("/topic/notifications" + (userId != null ? "-" + userId : ""), notification);
    }
}

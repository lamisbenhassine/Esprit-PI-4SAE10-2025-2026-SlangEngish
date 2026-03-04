package tn.esprit.gestioncours.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.gestioncours.DTO.NotificationResponseDto;
import tn.esprit.gestioncours.Services.INotificationService;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping("/{userId}")
    public List<NotificationResponseDto> getNotifications(@PathVariable Long userId) {
        return notificationService.getNotificationsForUser(userId);
    }

    @GetMapping("/unread/{userId}")
    public List<NotificationResponseDto> getUnreadNotifications(@PathVariable Long userId) {
        return notificationService.getUnreadNotificationsForUser(userId);
    }

    @PutMapping("/mark-as-read/{id}")
    public ResponseEntity<NotificationResponseDto> markAsRead(@PathVariable Long id) {
        NotificationResponseDto updated = notificationService.markAsRead(id);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}


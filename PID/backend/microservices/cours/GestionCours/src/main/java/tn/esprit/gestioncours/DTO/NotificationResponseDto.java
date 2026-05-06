package tn.esprit.gestioncours.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.gestioncours.Entities.NotificationType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private Long id;
    private Long userId;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
    private NotificationType type;
}


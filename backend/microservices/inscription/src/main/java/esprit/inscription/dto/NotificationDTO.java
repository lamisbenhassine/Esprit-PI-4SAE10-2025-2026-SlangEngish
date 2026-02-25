package esprit.inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Notification DTO — pushed to individual users or broadcast via WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    public enum Type {
        SUCCESS,    // Green — e.g. payment confirmed
        INFO,       // Blue  — e.g. new plan available
        WARNING,    // Orange — e.g. subscription expiring soon
        ERROR       // Red   — e.g. payment failed
    }

    private String id;          // unique id (UUID)
    private Type type;
    private String title;
    private String message;
    private String icon;        // Material icon name
    private String timestamp;   // ISO-8601
    private Long userId;        // null = broadcast to all
    private boolean read;
}

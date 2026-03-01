package esprit.inscription.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    // Broadcast campaign updates to all subscribers
    public void broadcastCampaignUpdate(String message) {
        log.info("Broadcasting campaign update: {}", message);
        messagingTemplate.convertAndSend("/topic/campaign-updates", message);
    }

    // Send user-specific notifications
    public void sendUserNotification(Long userId, String message) {
        log.info("Sending notification to user {}: {}", userId, message);
        messagingTemplate.convertAndSend("/queue/user-" + userId + "-notifications", message);
    }

    // Handle real-time email tracking updates
    @MessageMapping("/email-tracking")
    @SendTo("/topic/email-tracking")
    public String handleEmailTrackingUpdate(String trackingData) {
        log.info("Received email tracking update: {}", trackingData);
        return trackingData;
    }

    // Handle campaign status updates
    @MessageMapping("/campaign-status")
    @SendTo("/topic/campaign-status")
    public String handleCampaignStatusUpdate(String statusData) {
        log.info("Received campaign status update: {}", statusData);
        return statusData;
    }

    // Send real-time analytics updates
    public void broadcastAnalyticsUpdate(String analyticsData) {
        log.info("Broadcasting analytics update");
        messagingTemplate.convertAndSend("/topic/analytics", analyticsData);
    }

    // Send user engagement updates
    public void sendUserEngagementUpdate(Long userId, String engagementData) {
        log.info("Sending engagement update to user {}: {}", userId, engagementData);
        messagingTemplate.convertAndSend("/queue/user-" + userId + "-engagement", engagementData);
    }
}

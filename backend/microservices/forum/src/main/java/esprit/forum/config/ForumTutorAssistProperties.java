package esprit.forum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "forum.tutor-assist")
public class ForumTutorAssistProperties {

    /**
     * Base URL n8n (example: http://localhost:5678/webhook).
     */
    private String baseUrl = "";

    /**
     * Optional shared key sent to n8n on outgoing calls.
     */
    private String apiKey = "";

    /**
     * Relative path for slots lookup (default: calendar_slots).
     */
    private String slotsPath = "calendar_slots";

    /**
     * Relative path for availability check.
     */
    private String availabilityPath = "calendar_availability";

    /**
     * Relative path for appointment booking.
     */
    private String setAppointmentPath = "calendar_set_appointment";

    /**
     * Shared key expected from n8n callback (optional but recommended).
     */
    private String callbackKey = "";

    /**
     * VAPI public key exposed to front-office for web calls.
     */
    private String vapiPublicKey = "";

    /**
     * VAPI assistant id used by front-office call button.
     */
    private String vapiAssistantId = "";
}

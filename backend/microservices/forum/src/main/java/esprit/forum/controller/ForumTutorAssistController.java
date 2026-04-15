package esprit.forum.controller;

import esprit.forum.config.ForumTutorAssistProperties;
import esprit.forum.dto.TutorAssistAppointmentCallbackRequest;
import esprit.forum.dto.TutorAssistAvailabilityRequest;
import esprit.forum.dto.TutorAssistAvailabilityResponse;
import esprit.forum.dto.TutorAssistBookRequest;
import esprit.forum.dto.TutorAssistBookResponse;
import esprit.forum.dto.TutorAssistSlotsRequest;
import esprit.forum.dto.TutorAssistSlotsResponse;
import esprit.forum.service.ForumTutorAssistService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/forum/tutor-assist")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumTutorAssistController {

    private final ForumTutorAssistService tutorAssistService;
    private final ForumTutorAssistProperties tutorAssistProperties;
    private final Environment environment;

    @PostMapping("/slots")
    public ResponseEntity<?> slots(@RequestBody TutorAssistSlotsRequest request) {
        try {
            TutorAssistSlotsResponse out = tutorAssistService.getSlots(request);
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/availability")
    public ResponseEntity<?> availability(@RequestBody TutorAssistAvailabilityRequest request) {
        try {
            TutorAssistAvailabilityResponse out = tutorAssistService.checkAvailability(request);
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/book")
    public ResponseEntity<?> book(@RequestBody TutorAssistBookRequest request) {
        try {
            TutorAssistBookResponse out = tutorAssistService.bookAppointment(request);
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Callback n8n -> forum après création du rendez-vous pour publier un message automatique dans le sujet.
     */
    @PostMapping("/appointment-callback")
    public ResponseEntity<?> appointmentCallback(
            @RequestHeader(value = "X-Forum-Integration-Key", required = false) String integrationKey,
            @RequestBody TutorAssistAppointmentCallbackRequest request) {
        try {
            tutorAssistService.publishAppointmentCallback(request, integrationKey);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Public runtime config used by front-office to start VAPI web calls.
     */
    @GetMapping("/vapi-config")
    public ResponseEntity<Map<String, Object>> vapiConfig() {
        String publicKey = tutorAssistProperties.getVapiPublicKey();
        if (!StringUtils.hasText(publicKey)) {
            publicKey = environment.getProperty("vapiPublicKey", "");
        }
        String assistantId = tutorAssistProperties.getVapiAssistantId();
        if (!StringUtils.hasText(assistantId)) {
            assistantId = environment.getProperty("vapiAssistantId", "");
        }
        return ResponseEntity.ok(Map.of(
                "publicKey", publicKey == null ? "" : publicKey.trim(),
                "assistantId", assistantId == null ? "" : assistantId.trim()));
    }
}

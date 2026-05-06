package esprit.forum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import esprit.forum.config.ForumTutorAssistProperties;
import esprit.forum.dto.TutorAssistAppointmentCallbackRequest;
import esprit.forum.dto.TutorAssistAvailabilityRequest;
import esprit.forum.dto.TutorAssistAvailabilityResponse;
import esprit.forum.dto.TutorAssistBookRequest;
import esprit.forum.dto.TutorAssistBookResponse;
import esprit.forum.dto.TutorAssistSlotDto;
import esprit.forum.dto.TutorAssistSlotsRequest;
import esprit.forum.dto.TutorAssistSlotsResponse;
import esprit.forum.entity.ForumMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ForumTutorAssistService {

    private final ForumTutorAssistProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ForumMessageService forumMessageService;

    public TutorAssistSlotsResponse getSlots(TutorAssistSlotsRequest request) {
        Map<?, ?> body = postToN8n(props.getSlotsPath(), request);
        List<TutorAssistSlotDto> slots = extractSlots(body);
        return new TutorAssistSlotsResponse(slots);
    }

    public TutorAssistAvailabilityResponse checkAvailability(TutorAssistAvailabilityRequest request) {
        Map<?, ?> body = postToN8n(props.getAvailabilityPath(), request);
        Object available = body.get("available");
        boolean ok = available instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(available));
        return new TutorAssistAvailabilityResponse(ok);
    }

    public TutorAssistBookResponse bookAppointment(TutorAssistBookRequest request) {
        Map<?, ?> body = postToN8n(props.getSetAppointmentPath(), request);
        Object successRaw = body.get("success");
        boolean success = successRaw == null || Boolean.parseBoolean(String.valueOf(successRaw));
        String appointmentId = body.get("appointmentId") == null ? null : String.valueOf(body.get("appointmentId"));
        String meetLink = body.get("meetLink") == null ? null : String.valueOf(body.get("meetLink"));
        return new TutorAssistBookResponse(success, appointmentId, meetLink);
    }

    public void publishAppointmentCallback(TutorAssistAppointmentCallbackRequest request, String keyHeader) {
        if (StringUtils.hasText(props.getCallbackKey()) && !props.getCallbackKey().equals(keyHeader)) {
            throw new IllegalArgumentException("Invalid callback key");
        }
        if (request.getTopicId() == null) {
            throw new IllegalArgumentException("topicId is required");
        }
        String content = buildForumMessage(request);
        ForumMessage m = new ForumMessage();
        m.setAuthorId(request.getTutorId() != null ? request.getTutorId() : 1L);
        m.setContent(content);
        m.setParentMessageId(null);
        forumMessageService.createMessage(m, request.getTopicId());
    }

    private String buildForumMessage(TutorAssistAppointmentCallbackRequest r) {
        String tutor = StringUtils.hasText(r.getTutorName()) ? r.getTutorName() : ("Tutor #" + r.getTutorId());
        String focus = StringUtils.hasText(r.getProblemType()) ? r.getProblemType() : "general support";
        String tz = StringUtils.hasText(r.getTimezone()) ? r.getTimezone() : "UTC";
        String start = StringUtils.hasText(r.getStart()) ? r.getStart() : "-";
        String end = StringUtils.hasText(r.getEnd()) ? r.getEnd() : "-";
        String link = StringUtils.hasText(r.getMeetLink()) ? r.getMeetLink() : "(to be shared)";
        return "Tutor session confirmed.\n"
                + "Tutor: " + tutor + "\n"
                + "Start: " + start + "\n"
                + "End: " + end + " (" + tz + ")\n"
                + "Focus: " + focus + "\n"
                + "Meeting link: " + link;
    }

    private Map<?, ?> postToN8n(String path, Object payload) {
        if (!StringUtils.hasText(props.getBaseUrl())) {
            throw new IllegalStateException("Tutor assist is not configured (forum.tutor-assist.base-url)");
        }
        String url = buildUrl(path);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(props.getApiKey())) {
            headers.set("X-Forum-Integration-Key", props.getApiKey());
        }
        try {
            Object out = restTemplate.postForObject(url, new HttpEntity<>(payload, headers), Object.class);
            if (!(out instanceof Map<?, ?> m)) {
                return Map.of();
            }
            return m;
        } catch (RestClientResponseException e) {
            int s = e.getStatusCode() != null ? e.getStatusCode().value() : 0;
            throw new IllegalStateException("n8n webhook failed (HTTP " + s + ")", e);
        } catch (Exception e) {
            throw new IllegalStateException("n8n webhook failed: " + e.getMessage(), e);
        }
    }

    /**
     * n8n peut renvoyer différentes formes selon le node final (Code / Set / Respond).
     * Cette méthode accepte les variantes fréquentes :
     * - { "slots": [ ... ] }
     * - { "slots": { "slots": [ ... ] } }
     * - { "data": { "slots": [ ... ] } }
     * - [ {start,end,...}, ... ]
     * - [ {"json": {start,end,...}}, ... ]
     */
    private List<TutorAssistSlotDto> extractSlots(Map<?, ?> body) {
        List<TutorAssistSlotDto> out = new ArrayList<>();

        Object direct = body.get("slots");
        addSlotsFromUnknown(direct, out);
        if (!out.isEmpty()) {
            return out;
        }

        Object data = body.get("data");
        if (data instanceof Map<?, ?> m) {
            addSlotsFromUnknown(m.get("slots"), out);
            if (!out.isEmpty()) {
                return out;
            }
        }

        addSlotsFromUnknown(body, out);
        return out;
    }

    private void addSlotsFromUnknown(Object raw, List<TutorAssistSlotDto> out) {
        if (raw == null) {
            return;
        }

        if (raw instanceof Collection<?> coll) {
            for (Object item : coll) {
                if (item instanceof Map<?, ?> map && map.containsKey("json")) {
                    addOneSlot(map.get("json"), out);
                } else {
                    addOneSlot(item, out);
                }
            }
            return;
        }

        if (raw instanceof Map<?, ?> map) {
            if (map.containsKey("slots")) {
                addSlotsFromUnknown(map.get("slots"), out);
                return;
            }
            if (map.containsKey("start") && map.containsKey("end")) {
                addOneSlot(map, out);
            }
        }
    }

    private void addOneSlot(Object item, List<TutorAssistSlotDto> out) {
        try {
            TutorAssistSlotDto s = objectMapper.convertValue(item, TutorAssistSlotDto.class);
            if (StringUtils.hasText(s.getStart()) && StringUtils.hasText(s.getEnd())) {
                out.add(s);
            }
        } catch (IllegalArgumentException ignored) {
            // Ignore malformed slot entries.
        }
    }

    private String buildUrl(String path) {
        String b = props.getBaseUrl().trim();
        String p = (path == null ? "" : path.trim());
        if (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        return b + "/" + p;
    }
}

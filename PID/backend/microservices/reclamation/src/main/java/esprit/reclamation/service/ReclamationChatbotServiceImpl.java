package esprit.reclamation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import esprit.reclamation.dto.ChatbotAssistRequest;
import esprit.reclamation.dto.ChatbotAssistResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReclamationChatbotServiceImpl implements ReclamationChatbotService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${chatbot.openai.api-key:}")
    private String apiKey;

    @Value("${chatbot.openai.base-url:https://api.openai.com/v1/chat/completions}")
    private String baseUrl;

    @Value("${chatbot.openai.model:gpt-4o-mini}")
    private String model;

    @Override
    public ChatbotAssistResponse assist(ChatbotAssistRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackResponse(request.getMessage(), request.getSujet(), request.getDescription());
        }

        try {
            String userPrompt = buildUserPrompt(request);
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0.4);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", buildSystemPrompt()),
                    Map.of("role", "user", "content", userPrompt)
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, entity, String.class);
            return parseModelResponse(response.getBody(), request);
        } catch (Exception ex) {
            return fallbackResponse(request.getMessage(), request.getSujet(), request.getDescription());
        }
    }

    private ChatbotAssistResponse parseModelResponse(String body, ChatbotAssistRequest request) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull()) {
            return fallbackResponse(request.getMessage(), request.getSujet(), request.getDescription());
        }

        JsonNode parsed = objectMapper.readTree(contentNode.asText());
        String reply = parsed.path("reply").asText("I can help you refine your complaint.");
        String suggestedSubject = parsed.path("suggestedSubject").asText(request.getSujet() == null ? "" : request.getSujet());
        String suggestedDescription = parsed.path("suggestedDescription").asText(request.getDescription() == null ? "" : request.getDescription());

        return new ChatbotAssistResponse(reply, suggestedSubject, suggestedDescription, true);
    }

    private String buildSystemPrompt() {
        return "You are a complaint assistant for a school platform. " +
                "Always answer in JSON only with keys: reply, suggestedSubject, suggestedDescription. " +
                "Keep reply concise (max 2 sentences), professional, and helpful. " +
                "Improve clarity of complaint text.";
    }

    private String buildUserPrompt(ChatbotAssistRequest request) {
        return "User message: " + safe(request.getMessage()) + "\n" +
                "Current subject: " + safe(request.getSujet()) + "\n" +
                "Current description: " + safe(request.getDescription()) + "\n" +
                "Return JSON only.";
    }

    private ChatbotAssistResponse fallbackResponse(String message, String sujet, String description) {
        String lower = message == null ? "" : message.toLowerCase();
        String suggestedSubject = sujet == null ? "" : sujet;
        String suggestedDescription = description == null ? "" : description;
        String reply = "I refined your complaint draft. You can edit and submit.";

        if (lower.contains("payment") || lower.contains("paid")) {
            suggestedSubject = suggestedSubject.isBlank() ? "Payment issue" : suggestedSubject;
            suggestedDescription = suggestedDescription.isBlank()
                    ? "I completed a payment but my access is still blocked. Please verify my payment status."
                    : suggestedDescription;
        } else if (lower.contains("access") || lower.contains("login")) {
            suggestedSubject = suggestedSubject.isBlank() ? "Access problem" : suggestedSubject;
            suggestedDescription = suggestedDescription.isBlank()
                    ? "I cannot access my account/course. Please check and restore my access."
                    : suggestedDescription;
        } else if (lower.contains("bug") || lower.contains("error")) {
            suggestedSubject = suggestedSubject.isBlank() ? "Technical issue" : suggestedSubject;
            suggestedDescription = suggestedDescription.isBlank()
                    ? "I encountered a technical issue on the platform. Please investigate and fix it."
                    : suggestedDescription;
        } else {
            if (suggestedSubject.isBlank()) suggestedSubject = "General complaint";
            if (suggestedDescription.isBlank()) {
                suggestedDescription = message == null ? "" : message.trim();
            }
        }

        return new ChatbotAssistResponse(reply, suggestedSubject, suggestedDescription, false);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

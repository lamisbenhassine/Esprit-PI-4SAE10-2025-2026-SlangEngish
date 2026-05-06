package org.example.club.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostCommentAiService {

    public record ReviewResult(String correctedText, String sentiment, boolean corrected, boolean translatedToEnglish) {}

    private final RestTemplate ollamaRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ollama.enabled:true}")
    private boolean ollamaEnabled;

    @Value("${app.ollama.base-url:http://127.0.0.1:11434}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.model:llama3}")
    private String ollamaModel;

    public PostCommentAiService(
            @Qualifier("ollamaRestTemplate") RestTemplate ollamaRestTemplate,
            ObjectMapper objectMapper) {
        this.ollamaRestTemplate = ollamaRestTemplate;
        this.objectMapper = objectMapper;
    }

    public ReviewResult reviewComment(String text, boolean translateToEnglish) {
        String clean = text == null ? "" : text.trim();
        if (!ollamaEnabled || clean.isEmpty()) {
            return new ReviewResult(clean, "NEUTRAL", false, false);
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", ollamaModel);
            body.put("stream", false);
            body.put("format", "json");
            body.put("messages", List.of(Map.of("role", "user", "content", buildPrompt(clean, translateToEnglish))));
            Map<String, Object> options = new LinkedHashMap<>();
            options.put("temperature", 0.1);
            body.put("options", options);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = ollamaRestTemplate.postForObject(
                    ollamaBaseUrl.replaceAll("/$", "") + "/api/chat",
                    entity,
                    Map.class);

            if (resp == null || !(resp.get("message") instanceof Map<?, ?> msg)) {
                return new ReviewResult(clean, "NEUTRAL", false, false);
            }
            Object contentRaw = msg.get("content");
            String content = contentRaw == null ? "" : String.valueOf(contentRaw).trim();
            if (content.isEmpty()) {
                return new ReviewResult(clean, "NEUTRAL", false, false);
            }

            JsonNode root = objectMapper.readTree(content);
            String correctedText = root.path("correctedText").asText("").trim();
            String sentiment = root.path("sentiment").asText("NEUTRAL").trim().toUpperCase();
            if (!"POSITIVE".equals(sentiment) && !"NEGATIVE".equals(sentiment)) {
                sentiment = "NEUTRAL";
            }
            if (correctedText.isEmpty()) {
                correctedText = clean;
            }
            boolean corrected = !clean.equals(correctedText);
            boolean translated = translateToEnglish && !clean.equalsIgnoreCase(correctedText);
            return new ReviewResult(correctedText, sentiment, corrected, translated);
        } catch (Exception ex) {
            return new ReviewResult(clean, "NEUTRAL", false, false);
        }
    }

    private static String buildPrompt(String text, boolean translateToEnglish) {
        return "Tu reçois un commentaire étudiant. Réponds UNIQUEMENT en JSON UTF-8 valide, sans markdown, avec la structure:"
                + "{\"correctedText\":\"...\",\"sentiment\":\"POSITIVE|NEUTRAL|NEGATIVE\"}.\n"
                + "Règles:\n"
                + "- Corrige seulement orthographe, accents et ponctuation.\n"
                + "- Garde le même sens et un ton naturel.\n"
                + (translateToEnglish
                    ? "- Traduis le résultat final en anglais naturel (sans changer le sens).\n"
                    : "- Ne traduis JAMAIS le texte, conserve strictement la langue d'origine.\n")
                + "- sentiment doit représenter le ton global du commentaire.\n"
                + "Commentaire: " + text;
    }
}

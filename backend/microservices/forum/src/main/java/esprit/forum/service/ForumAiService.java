package esprit.forum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import esprit.forum.config.ForumAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ForumAiService {

    private final ForumAiProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public String summarizeEnglish(String rawText) {
        String text = validateInputText(rawText);
        ensureApiKeyConfigured();

        String prompt =
                "Summarize the following text in clear English as 3 to 5 very short bullet points. "
                        + "Each bullet must be one phrase only (max ~18 words per bullet). "
                        + "Total output under 90 words. Capture main ideas only — do NOT paste or paraphrase long stretches of the original. "
                        + "Start each line with \"- \". No title, no introduction line, no closing commentary. "
                        + "Text:\n"
                        + text;

        try {
            return callGemini(prompt, 256, 0.2);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode() != null ? e.getStatusCode().value() : 0;
            throw new IllegalStateException("AI provider error: HTTP " + status, e);
        } catch (Exception e) {
            throw new IllegalStateException("AI summarization failed", e);
        }
    }

    /**
     * Translates arbitrary text to natural English via Gemini (same API key as summarize).
     */
    public String translateToEnglish(String rawText) {
        String text = validateInputText(rawText);
        ensureApiKeyConfigured();

        String prompt =
                "Translate the following text to natural English. "
                        + "If it is already in English, return it unchanged or with only light fixes for clarity. "
                        + "Preserve meaning and tone. "
                        + "Output only the translated text — no quotes, labels, or commentary.\n\n"
                        + text;

        try {
            return callGemini(prompt, 2048, 0.2);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode() != null ? e.getStatusCode().value() : 0;
            throw new IllegalStateException("AI provider error: HTTP " + status, e);
        } catch (Exception e) {
            throw new IllegalStateException("AI translation failed", e);
        }
    }

    private String validateInputText(String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("text is required");
        }
        if (text.length() > props.getMaxInputChars()) {
            throw new IllegalArgumentException("text is too long");
        }
        return text;
    }

    private void ensureApiKeyConfigured() {
        if (!StringUtils.hasText(props.getApiKey())) {
            throw new IllegalStateException("AI is not configured (missing forum.ai.api-key / FORUM_AI_API_KEY)");
        }
    }

    private String callGemini(String prompt, int maxOutputTokens, double temperature) throws Exception {
        String url = UriComponentsBuilder
                .fromUriString("https://generativelanguage.googleapis.com/v1beta/models/" + props.getModel() + ":generateContent")
                .queryParam("key", props.getApiKey())
                .build()
                .toUriString();

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", maxOutputTokens
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = objectMapper.writeValueAsString(body);

        ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(json, headers), String.class);
        return extractTextFromGeminiResponse(resp.getBody());
    }

    private String extractTextFromGeminiResponse(String responseBody) throws Exception {
        if (!StringUtils.hasText(responseBody)) {
            throw new IllegalStateException("Empty AI response");
        }
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("Invalid AI response (no candidates)");
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new IllegalStateException("Invalid AI response (no parts)");
        }
        String text = parts.get(0).path("text").asText("").trim();
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException("Invalid AI response (empty text)");
        }
        return text;
    }
}


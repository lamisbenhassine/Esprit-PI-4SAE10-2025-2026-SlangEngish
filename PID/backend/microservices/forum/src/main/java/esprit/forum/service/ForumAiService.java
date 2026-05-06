package esprit.forum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import esprit.forum.config.ForumAiProperties;
import esprit.forum.dto.AiTranslateTopicResponse;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatusCode;

@Service
@RequiredArgsConstructor
public class ForumAiService {

    private static final Set<String> ALLOWED_TARGET_LANGS = Set.of(
            "en", "fr", "ar", "es", "de", "it", "pt", "tr", "nl", "pl", "ru", "ja", "zh", "hi", "vi", "ko", "sv", "da", "no", "fi"
    );

    private final ForumAiProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Orthographe, grammaire et anglais familier → anglais clair (forum / messagerie).
     * Réponse : texte corrigé uniquement (pas de commentaire).
     */
    public String polishEnglishCompose(String rawText) {
        String text = validateInputText(rawText);
        ensureApiKeyConfigured();

        String prompt =
                "You are an English writing coach for learners. Improve the following text:\n"
                        + "- Fix spelling and grammar.\n"
                        + "- Turn heavy slang into clear standard English when it helps (e.g. \"wanna\" → \"want to\", "
                        + "\"someth\" → \"something\"), but keep a friendly student tone.\n"
                        + "- Preserve meaning, names, URLs, @handles, hashtags, and code-like tokens unchanged.\n"
                        + "- Keep line breaks where they help readability.\n"
                        + "- Output ONLY the improved text. No title, no quotes, no explanation before or after.\n\n"
                        + text;

        try {
            return callGemini(prompt, 2048, 0.12);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode() != null ? e.getStatusCode().value() : 0;
            throw new IllegalStateException("AI provider error: HTTP " + status, e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "AI polish failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                    e);
        }
    }

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
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "AI summarization failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                    e);
        }
    }

    /**
     * Translates arbitrary text to a target language (ISO 639-1) via Gemini.
     */
    public String translateToLanguage(String rawText, String targetLanguageCode) {
        String text = validateInputText(rawText);
        ensureApiKeyConfigured();
        String lang = normalizeTargetLanguage(targetLanguageCode);
        String langLabel = humanLanguageName(lang);

        String prompt =
                "Translate the following text to natural "
                        + langLabel
                        + " (language code "
                        + lang
                        + "). "
                        + "If it is already mostly in that language, return it unchanged or with only light fixes for clarity. "
                        + "Preserve meaning and tone. "
                        + "Output only the translated text — no quotes, labels, or commentary.\n\n"
                        + text;

        try {
            return callGemini(prompt, 2048, 0.2);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode() != null ? e.getStatusCode().value() : 0;
            throw new IllegalStateException("AI provider error: HTTP " + status, e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "AI translation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                    e);
        }
    }

    /** Rétrocompatibilité : équivalent à {@code translateToLanguage(rawText, "en")}. */
    public String translateToEnglish(String rawText) {
        return translateToLanguage(rawText, "en");
    }

    /**
     * Traduit titre + corps en un seul appel Gemini (réduit fortement les erreurs 429 vs 2 appels).
     */
    public AiTranslateTopicResponse translateTopicTitleAndDescription(
            String rawTitle,
            String rawBody,
            String targetLanguageCode) {
        String title = rawTitle == null ? "" : rawTitle.trim();
        String body = rawBody == null ? "" : rawBody.trim();
        if (!StringUtils.hasText(title) && !StringUtils.hasText(body)) {
            throw new IllegalArgumentException("title or description required");
        }
        int combined = title.length() + body.length();
        if (combined > props.getMaxInputChars()) {
            throw new IllegalArgumentException("title and description combined exceed max length");
        }
        ensureApiKeyConfigured();
        String lang = normalizeTargetLanguage(targetLanguageCode);
        String langLabel = humanLanguageName(lang);

        String prompt =
                "You translate forum posts into natural "
                        + langLabel
                        + " (ISO language code "
                        + lang
                        + "). Preserve meaning and tone.\n\n"
                        + "Reply with ONLY one JSON object. No markdown code fences, no text before or after the JSON. "
                        + "Use exactly these keys: \"trTitle\", \"trDescription\".\n\n"
                        + "Rules:\n"
                        + "- trTitle: translated title, or \"\" if there was no title.\n"
                        + "- trDescription: translated body, or \"\" if there was no body.\n"
                        + "- Escape inner quotes and line breaks in JSON strings correctly.\n\n"
                        + "Original title:\n"
                        + title
                        + "\n\nOriginal body:\n"
                        + body;

        try {
            String rawJson = callGemini(prompt, 8192, 0.2);
            try {
                return parseTopicTranslationJson(rawJson);
            } catch (Exception parseErr) {
                String trTitle = StringUtils.hasText(title) ? translateToLanguage(title, lang) : "";
                String trDesc = StringUtils.hasText(body) ? translateToLanguage(body, lang) : "";
                if (!StringUtils.hasText(trTitle) && !StringUtils.hasText(trDesc)) {
                    throw parseErr;
                }
                return new AiTranslateTopicResponse(trTitle, trDesc);
            }
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode() != null ? e.getStatusCode().value() : 0;
            throw new IllegalStateException("AI provider error: HTTP " + status, e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "AI topic translation failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                    e);
        }
    }

    private AiTranslateTopicResponse parseTopicTranslationJson(String modelOutput) throws Exception {
        String s = stripMarkdownJsonFence(modelOutput.trim());
        JsonNode root;
        try {
            root = objectMapper.readTree(s);
        } catch (Exception ex) {
            int i = s.indexOf('{');
            int j = s.lastIndexOf('}');
            if (i >= 0 && j > i) {
                root = objectMapper.readTree(s.substring(i, j + 1));
            } else {
                throw ex;
            }
        }
        String trTitle = root.path("trTitle").asText("").trim();
        String trDesc = root.path("trDescription").asText("").trim();
        if (!StringUtils.hasText(trTitle) && root.has("tr_title")) {
            trTitle = root.path("tr_title").asText("").trim();
        }
        if (!StringUtils.hasText(trDesc) && root.has("tr_description")) {
            trDesc = root.path("tr_description").asText("").trim();
        }
        if (!StringUtils.hasText(trTitle) && !StringUtils.hasText(trDesc)) {
            throw new IllegalStateException("Empty batch translation (could not parse JSON keys trTitle/trDescription)");
        }
        return new AiTranslateTopicResponse(trTitle, trDesc);
    }

    private static String stripMarkdownJsonFence(String text) {
        String s = text.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) {
                s = s.substring(firstNl + 1);
            }
            int fence = s.lastIndexOf("```");
            if (fence >= 0) {
                s = s.substring(0, fence);
            }
        }
        return s.trim();
    }

    private String normalizeTargetLanguage(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("targetLanguage is required (ISO 639-1, e.g. fr, en, ar)");
        }
        String code = raw.trim().toLowerCase(Locale.ROOT);
        if (code.length() > 2) {
            code = code.substring(0, 2);
        }
        if (!ALLOWED_TARGET_LANGS.contains(code)) {
            throw new IllegalArgumentException("Unsupported target language: " + raw);
        }
        return code;
    }

    private String humanLanguageName(String iso2) {
        return switch (iso2) {
            case "en" -> "English";
            case "fr" -> "French";
            case "ar" -> "Arabic";
            case "es" -> "Spanish";
            case "de" -> "German";
            case "it" -> "Italian";
            case "pt" -> "Portuguese";
            case "tr" -> "Turkish";
            case "nl" -> "Dutch";
            case "pl" -> "Polish";
            case "ru" -> "Russian";
            case "ja" -> "Japanese";
            case "zh" -> "Chinese (Simplified)";
            case "hi" -> "Hindi";
            case "vi" -> "Vietnamese";
            case "ko" -> "Korean";
            case "sv" -> "Swedish";
            case "da" -> "Danish";
            case "no" -> "Norwegian";
            case "fi" -> "Finnish";
            default -> iso2.toUpperCase(Locale.ROOT);
        };
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

    /**
     * Appelle Gemini avec retries sur 429 / 503. Délais limités : un proxy / navigateur trop long → ERR_EMPTY_RESPONSE.
     */
    private String callGemini(String prompt, int maxOutputTokens, double temperature) throws Exception {
        final int maxAttempts = 2;
        final long baseDelayMs = 1_200L;
        final long maxWaitPerAttemptMs = 6_000L;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callGeminiOnce(prompt, maxOutputTokens, temperature);
            } catch (RestClientResponseException e) {
                HttpStatusCode code = e.getStatusCode();
                int status = code != null ? code.value() : 0;
                if ((status == 429 || status == 503) && attempt < maxAttempts) {
                    long fromHeader = parseRetryAfterMillis(e);
                    long exponential = Math.min(baseDelayMs * (1L << (attempt - 1)), maxWaitPerAttemptMs);
                    long backoff = fromHeader > 0 ? Math.min(fromHeader, 45_000L) : exponential;
                    backoff = Math.max(backoff, 1_500L);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Translation interrupted", ie);
                    }
                    continue;
                }
                String geminiHint = extractGeminiErrorMessageFromHttpBody(safeResponseBody(e));
                throw new IllegalStateException(
                        "AI provider error: HTTP "
                                + status
                                + (StringUtils.hasText(geminiHint) ? (" — " + geminiHint) : ""),
                        e);
            }
        }
        throw new IllegalStateException("AI provider error: exhausted retries");
    }

    /** Valeur Retry-After en millisecondes si entier (secondes), sinon -1. */
    private static long parseRetryAfterMillis(RestClientResponseException e) {
        try {
            HttpHeaders h = e.getResponseHeaders();
            if (h == null) {
                return -1;
            }
            String v = h.getFirst(HttpHeaders.RETRY_AFTER);
            if (!StringUtils.hasText(v)) {
                return -1;
            }
            return Long.parseLong(v.trim()) * 1000L;
        } catch (NumberFormatException | ArithmeticException ex) {
            return -1;
        }
    }

    private String callGeminiOnce(String prompt, int maxOutputTokens, double temperature) throws Exception {
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
        JsonNode err = root.path("error");
        if (err.isObject() && !err.isMissingNode()) {
            String msg = err.path("message").asText("").trim();
            String status = err.path("status").asText("").trim();
            String detail = StringUtils.hasText(msg) ? msg : responseBody;
            if (StringUtils.hasText(status)) {
                throw new IllegalStateException("Gemini API error (" + status + "): " + detail);
            }
            throw new IllegalStateException("Gemini API error: " + detail);
        }
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("Invalid AI response (no candidates). Check model name and API key.");
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

    private static String safeResponseBody(RestClientResponseException e) {
        try {
            return e.getResponseBodyAsString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String extractGeminiErrorMessageFromHttpBody(String body) {
        if (!StringUtils.hasText(body)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode err = root.path("error");
            if (err.isObject()) {
                String m = err.path("message").asText("").trim();
                if (StringUtils.hasText(m) && m.length() < 800) {
                    return m;
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return "";
    }
}


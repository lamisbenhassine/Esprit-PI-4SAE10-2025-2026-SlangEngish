package tn.esprit.gestioncours.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.gestioncours.DTO.NoteAnalysisResponseDto;
import tn.esprit.gestioncours.DTO.openai.NoteAiJsonPayload;
import tn.esprit.gestioncours.DTO.openai.OpenAiChatCompletionResponse;
import tn.esprit.gestioncours.Entities.ChapterNote;
import tn.esprit.gestioncours.Repositories.ChapterNoteRepository;

import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteAiAnalysisService {

    private static final String SYSTEM_PROMPT = """
            You are a warm, encouraging English-learning assistant. Students keep personal study notes (content may mix English and French).
            Tasks:
            1) Fix spelling and grammar in the note while preserving the student's intended meaning and language choices when they are learning examples.
            2) Reorganize the note for clarity: short paragraphs, logical flow, bullet lists where they help.
            3) If the current title is vague, generic, or empty (e.g. "Sans titre", "Note", "Untitled"), suggest one concise, specific title; otherwise set suggestedTitle to null.

            Return ONLY valid JSON with exactly these keys:
            - feedbackSummary (string, 2-4 short sentences, friendly tone, mention 1-2 concrete improvements without being harsh)
            - suggestedTitle (string or null)
            - grammarCorrectedHtml (string, HTML fragment using ONLY these tags: p, br, strong, em, u, ul, ol, li — grammar/spelling fixes, keep structure close to the original)
            - restructuredHtml (string, same allowed tags — final polished version after reorganization; must include the grammar fixes)

            Do not wrap the JSON in markdown fences.""";

    private final ChapterNoteRepository chapterNoteRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String openAiUrl;

    @Value("${openai.model:${openai.api.model:llama-3.1-8b-instant}}")
    private String openAiModel;

    @Value("${openai.fallback.mock-on-error:true}")
    private boolean mockOnOpenAiError;

    @PostConstruct
    void logResolvedLlmSettings() {
        log.info("NoteAiAnalysisService: URL={}, model={} — si Groq renvoie encore llama3-8b-8192, redémarrez GestionCours et vérifiez OPENAI_MODEL / openai.model dans l’environnement.",
                openAiUrl, openAiModel);
    }

    public Optional<NoteAnalysisResponseDto> analyzeForUser(Long noteId, Long userId) {
        Optional<ChapterNote> opt = chapterNoteRepository.findByIdAndUserId(noteId, userId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        ChapterNote note = opt.get();
        String originalTitle = note.getTitle() == null ? "" : note.getTitle();
        String originalHtml = note.getContent() == null ? "" : note.getContent();

        NoteAiJsonPayload ai = runLlm(originalTitle, originalHtml);

        return Optional.of(NoteAnalysisResponseDto.builder()
                .feedbackSummary(nullToEmpty(ai.getFeedbackSummary()))
                .suggestedTitle(emptyToNull(ai.getSuggestedTitle()))
                .grammarCorrectedHtml(nullToEmpty(ai.getGrammarCorrectedHtml()))
                .restructuredHtml(nullToEmpty(ai.getRestructuredHtml()))
                .originalTitle(originalTitle)
                .originalHtml(originalHtml)
                .build());
    }

    private NoteAiJsonPayload runLlm(String title, String html) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            log.warn("openai.api.key is not set — returning mock analysis for development");
            return mockPayload(title, html);
        }
        try {
            return callOpenAi(title, html);
        } catch (Exception e) {
            log.error("OpenAI analysis failed", e);
            if (mockOnOpenAiError) {
                return mockPayloadAfterOpenAiFailure(title, html, aggregateErrorHint(e));
            }
            throw new IllegalStateException(rootMessage(e), e);
        }
    }

    /** Concatène statut HTTP + messages de la chaîne (évite de rater un « 429 » enfoui dans une cause). */
    private static String aggregateErrorHint(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable t = e;
        for (int depth = 0; t != null && depth < 10; depth++) {
            if (t instanceof HttpStatusCodeException h) {
                sb.append("HTTP").append(h.getStatusCode().value()).append(' ');
            }
            String m = t.getMessage();
            if (m != null && !m.isBlank()) {
                sb.append(m).append(' ');
            }
            Throwable next = t.getCause();
            if (next == null || next == t) {
                break;
            }
            t = next;
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? e.getClass().getSimpleName() : truncate(out, 1200);
    }

    private static String rootMessage(Throwable e) {
        Throwable c = e;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        String m = c.getMessage();
        return m != null ? m : c.getClass().getSimpleName();
    }

    private NoteAiJsonPayload callOpenAi(String title, String html) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", openAiModel);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("temperature", 0.4);

        String userContent = "Current title: " + title + "\n\nHTML note content:\n" + html;

        body.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userContent)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey.trim());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<OpenAiChatCompletionResponse> response;
        try {
            response = restTemplate.postForEntity(openAiUrl, entity, OpenAiChatCompletionResponse.class);
        } catch (HttpStatusCodeException e) {
            String snippet = truncate(e.getResponseBodyAsString(), 800);
            throw new IllegalStateException(
                    "OpenAI HTTP " + e.getStatusCode().value() + (snippet.isBlank() ? "" : ": " + snippet), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("OpenAI network error: " + e.getMessage(), e);
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null
                || response.getBody().getChoices() == null
                || response.getBody().getChoices().isEmpty()
                || response.getBody().getChoices().get(0).getMessage() == null) {
            throw new IllegalStateException("Invalid OpenAI response (empty choices or body)");
        }

        String content = response.getBody().getChoices().get(0).getMessage().getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OpenAI returned empty message content");
        }
        String json = extractJson(content);
        try {
            return objectMapper.readValue(json, NoteAiJsonPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not parse AI JSON: " + truncate(json, 400), e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim().replaceAll("\\s+", " ");
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private static String extractJson(String content) {
        String trimmed = content.trim();
        Pattern fence = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher m = fence.matcher(trimmed);
        if (m.find()) {
            return m.group(1).trim();
        }
        return trimmed;
    }

    private static NoteAiJsonPayload mockPayload(String title, String html) {
        NoteAiJsonPayload p = new NoteAiJsonPayload();
        p.setFeedbackSummary(
                "Bravo pour vos prises de notes — tenir un carnet aide vraiment à progresser. "
                        + "Mode démo : aucune clé OpenAI n’est chargée. Pour une vraie analyse : "
                        + "(1) variable d’environnement OPENAI_API_KEY=sk-… dans la configuration d’exécution IntelliJ, "
                        + "ou (2) fichier src/main/resources/application-secret.properties avec une ligne openai.api.key=sk-… "
                        + "(sans ${ } autour de la clé — copier application-secret.properties.example). Puis redémarrer GestionCours.");
        p.setSuggestedTitle((title == null || title.isBlank() || title.equalsIgnoreCase("sans titre"))
                ? "Study notes — key points"
                : null);
        p.setGrammarCorrectedHtml(wrapIfPlain(html));
        p.setRestructuredHtml("<p><strong>Version réorganisée (aperçu local)</strong></p>" + wrapIfPlain(html));
        return p;
    }

    private static NoteAiJsonPayload mockPayloadAfterOpenAiFailure(String title, String html, String technicalHint) {
        NoteAiJsonPayload p = new NoteAiJsonPayload();
        String hint = technicalHint == null ? "" : technicalHint.toLowerCase();
        String summary;
        if (hint.contains("429")
                || hint.contains("http429")
                || hint.contains("quota")
                || hint.contains("exceeded your current quota")
                || hint.contains("insufficient_quota")
                || hint.contains("billing")
                || hint.contains("too many requests")) {
            summary =
                    "OpenAI a répondu « quota dépassé » (erreur 429) : il n’y a plus assez de crédit ou d’usage autorisé sur ce compte / ce projet. "
                            + "Rendez-vous sur https://platform.openai.com (facturation, limites, usage) pour ajouter du crédit ou vérifier votre forfait. "
                            + "Ce n’est pas un bug de l’application. En attendant, ci-dessous : votre note telle qu’enregistrée, sans analyse IA.";
        } else if (hint.contains("401")
                || hint.contains("invalid_api_key")
                || hint.contains("incorrect api key")) {
            summary =
                    "La clé API OpenAI a été refusée (401). Vérifiez la clé, le projet et les restrictions sur https://platform.openai.com/api-keys . "
                            + "Aperçu local de votre texte sans modification par l’IA.";
        } else {
            summary =
                    "Le service d’IA n’a pas pu répondre (réseau, erreur serveur ou réponse inattendue). "
                            + "Réessayez plus tard ou vérifiez la configuration. Aperçu local sans analyse IA. "
                            + "Indication : "
                            + truncate(technicalHint, 220);
        }
        p.setFeedbackSummary(summary);
        p.setSuggestedTitle(null);
        p.setGrammarCorrectedHtml(wrapIfPlain(html));
        p.setRestructuredHtml("<p><strong>Aperçu (sans IA)</strong></p>" + wrapIfPlain(html));
        return p;
    }

    private static String wrapIfPlain(String html) {
        if (html == null || html.isBlank()) {
            return "<p><em>(note vide)</em></p>";
        }
        if (html.trim().startsWith("<")) {
            return html;
        }
        return "<p>" + html.replace("\n", "<br/>") + "</p>";
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}

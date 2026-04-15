package esprit.forum.controller;

import esprit.forum.dto.AiPolishEnglishResponse;
import esprit.forum.dto.AiSummarizeRequest;
import esprit.forum.dto.AiSummarizeResponse;
import esprit.forum.dto.AiTranslateRequest;
import esprit.forum.dto.AiTranslateResponse;
import esprit.forum.dto.AiTranslateTopicRequest;
import esprit.forum.dto.AiTranslateTopicResponse;
import esprit.forum.service.ContentModerationService;
import esprit.forum.service.ForumAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestController
@RequestMapping("/api/forum/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumAiController {

    private final ForumAiService forumAiService;
    private final ContentModerationService contentModerationService;

    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(@RequestBody AiSummarizeRequest request) {
        try {
            String summary = forumAiService.summarizeEnglish(request == null ? null : request.getText());
            return ResponseEntity.ok(new AiSummarizeResponse(summary));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return aiErrorResponse(e);
        }
    }

    @PostMapping("/translate-to-english")
    public ResponseEntity<?> translateToEnglish(@RequestBody AiSummarizeRequest request) {
        try {
            String translated = forumAiService.translateToEnglish(request == null ? null : request.getText());
            return ResponseEntity.ok(new AiTranslateResponse(translated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return aiErrorResponse(e);
        }
    }

    /**
     * Traduction titre + corps en un seul appel IA (moins de risque 429 que deux POST /translate).
     */
    @PostMapping("/translate-topic")
    public ResponseEntity<?> translateTopic(@RequestBody AiTranslateTopicRequest request) {
        try {
            String title = request == null ? null : request.getTitle();
            String description = request == null ? null : request.getDescription();
            String lang = request == null ? null : request.getTargetLanguage();
            AiTranslateTopicResponse out =
                    forumAiService.translateTopicTitleAndDescription(title, description, lang);
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return aiErrorResponse(e);
        }
    }

    /**
     * Amélioration orthographe / grammaire / style (Gemini). Le texte d’entrée est aussi filtré
     * contre le vocabulaire interdit comme pour l’envoi d’un message.
     */
    @PostMapping("/polish-english")
    public ResponseEntity<?> polishEnglish(@RequestBody AiSummarizeRequest request) {
        try {
            String text = request == null ? null : request.getText();
            contentModerationService.assertTextAcceptable(text);
            String corrected = forumAiService.polishEnglishCompose(text);
            return ResponseEntity.ok(new AiPolishEnglishResponse(corrected));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return aiErrorResponse(e);
        }
    }

    /** Traduction vers une langue cible (liste validée côté serveur). */
    @PostMapping("/translate")
    public ResponseEntity<?> translate(@RequestBody AiTranslateRequest request) {
        try {
            String text = request == null ? null : request.getText();
            String lang = request == null ? null : request.getTargetLanguage();
            String translated = forumAiService.translateToLanguage(text, lang);
            return ResponseEntity.ok(new AiTranslateResponse(translated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return aiErrorResponse(e);
        }
    }

    private ResponseEntity<Map<String, String>> aiErrorResponse(IllegalStateException e) {
        Throwable c = e.getCause();
        if (c instanceof RestClientResponseException rce) {
            int status = rce.getStatusCode() != null ? rce.getStatusCode().value() : 0;
            if (status == 429) {
                String msg =
                        "Quota Google (429) : une nouvelle clé dans le même projet ne supprime pas la limite. "
                                + "Attendez 1–2 min, évitez les clics répétés, ou activez la facturation / un plan sur Google AI Studio.";
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", msg));
            }
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
    }
}

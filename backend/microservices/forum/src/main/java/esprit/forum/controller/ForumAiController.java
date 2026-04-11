package esprit.forum.controller;

import esprit.forum.dto.AiSummarizeRequest;
import esprit.forum.dto.AiSummarizeResponse;
import esprit.forum.dto.AiTranslateResponse;
import esprit.forum.service.ForumAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/forum/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumAiController {

    private final ForumAiService forumAiService;

    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(@RequestBody AiSummarizeRequest request) {
        try {
            String summary = forumAiService.summarizeEnglish(request == null ? null : request.getText());
            return ResponseEntity.ok(new AiSummarizeResponse(summary));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
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
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        }
    }
}

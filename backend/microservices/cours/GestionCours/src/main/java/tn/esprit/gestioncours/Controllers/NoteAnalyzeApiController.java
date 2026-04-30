package tn.esprit.gestioncours.Controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.gestioncours.DTO.NoteAnalysisErrorDto;
import tn.esprit.gestioncours.DTO.NoteAnalysisResponseDto;
import tn.esprit.gestioncours.Services.NoteAiAnalysisService;

import java.util.Optional;

/**
 * Exposé via la gateway sous {@code POST /api/notes/{id}/analyze?userId=}.
 */
@RestController
@RequestMapping("/chapter-note/api-notes")
@RequiredArgsConstructor
@Slf4j
public class NoteAnalyzeApiController {

    private final NoteAiAnalysisService noteAiAnalysisService;

    @PostMapping("/{id}/analyze")
    public ResponseEntity<?> analyze(
            @PathVariable("id") Long noteId,
            @RequestParam Long userId) {
        Optional<NoteAnalysisResponseDto> result;
        try {
            result = noteAiAnalysisService.analyzeForUser(noteId, userId);
        } catch (IllegalStateException e) {
            log.warn("Analyze error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new NoteAnalysisErrorDto("analysis_failed", e.getMessage()));
        }
        return result
                .map(r -> (ResponseEntity<?>) ResponseEntity.ok(r))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

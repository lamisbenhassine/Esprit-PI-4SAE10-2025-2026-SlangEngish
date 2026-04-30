package tn.esprit.gestioncours.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.gestioncours.DTO.ChapterLearningContentResponseDto;
import tn.esprit.gestioncours.Services.ChapterLearningContentService;

/**
 * Exposé via la gateway sous {@code /api/chapters/**} → GestionCours.
 */
@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
public class ChapterLearningContentApiController {

    private final ChapterLearningContentService learningContentService;

    @GetMapping("/{id}/learning-content")
    public ResponseEntity<ChapterLearningContentResponseDto> getCached(@PathVariable("id") Long chapterId) {
        return learningContentService.getCached(chapterId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Génère (ou renvoie le cache si {@code regenerate=false} et contenu déjà présent).
     */
    @PostMapping("/{id}/generate-content")
    public ResponseEntity<ChapterLearningContentResponseDto> generateContent(
            @PathVariable("id") Long chapterId,
            @RequestParam(value = "regenerate", defaultValue = "false") boolean regenerate) {
        try {
            ChapterLearningContentResponseDto dto = learningContentService.generateOrGet(chapterId, regenerate);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }
}

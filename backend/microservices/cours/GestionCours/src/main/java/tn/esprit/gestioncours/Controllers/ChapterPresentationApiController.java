package tn.esprit.gestioncours.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.gestioncours.DTO.ChapterPresentationResponseDto;
import tn.esprit.gestioncours.Services.PresentationService;

import java.nio.file.Path;

/**
 * Présentation animée + narration (PDF → Groq → ElevenLabs), exposée via la gateway sous {@code /api/chapters/**}.
 */
@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
public class ChapterPresentationApiController {

    private final PresentationService presentationService;

    @GetMapping("/{id}/presentation")
    public ResponseEntity<ChapterPresentationResponseDto> getPresentation(@PathVariable("id") Long chapterId) {
        return presentationService.getCached(chapterId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/{id}/generate-presentation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChapterPresentationResponseDto> generatePresentation(
            @PathVariable("id") Long chapterId,
            @RequestParam(value = "regenerate", defaultValue = "false") boolean regenerate,
            @RequestParam("file") MultipartFile file) {
        try {
            ChapterPresentationResponseDto dto = presentationService.generateFromPdf(chapterId, file, regenerate);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }
}

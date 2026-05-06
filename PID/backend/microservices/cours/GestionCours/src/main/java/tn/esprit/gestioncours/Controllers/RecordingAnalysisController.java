package tn.esprit.gestioncours.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.gestioncours.DTO.RecordingAnalysisStatusResponseDto;
import tn.esprit.gestioncours.DTO.RecordingHighlightDto;
import tn.esprit.gestioncours.Entities.RecordingAnalysisStatus;
import tn.esprit.gestioncours.Services.RecordingAnalysisAsyncService;
import tn.esprit.gestioncours.Services.RecordingAnalysisService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recordings")
@RequiredArgsConstructor
public class RecordingAnalysisController {

    private final RecordingAnalysisService recordingAnalysisService;
    private final RecordingAnalysisAsyncService recordingAnalysisAsyncService;

    @PostMapping("/{id}/analyze")
    public ResponseEntity<Map<String, String>> analyzeRecording(@PathVariable Long id) {
        boolean accepted = recordingAnalysisAsyncService.triggerAnalysisAsync(id);
        if (!accepted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("status", RecordingAnalysisStatus.PROCESSING.name()));
    }

    @GetMapping("/{id}/highlights")
    public ResponseEntity<List<RecordingHighlightDto>> getHighlights(@PathVariable Long id) {
        return ResponseEntity.ok(recordingAnalysisService.getHighlights(id));
    }

    @GetMapping("/{id}/analysis-status")
    public ResponseEntity<RecordingAnalysisStatusResponseDto> getAnalysisStatus(@PathVariable Long id) {
        return ResponseEntity.ok(new RecordingAnalysisStatusResponseDto(recordingAnalysisService.getStatus(id)));
    }
}

package org.example.evenement.controller;

import org.example.evenement.dto.FeedbackDto;
import org.example.evenement.dto.MoyenneFeedbackDto;
import org.example.evenement.service.FeedbackEvenementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class FeedbackController {

    @Autowired
    private FeedbackEvenementService feedbackService;

    @PostMapping
    public ResponseEntity<?> createOrUpdateFeedback(@RequestBody Map<String, Object> body) {
        try {
            Long idEtudiant = Long.valueOf(body.get("idEtudiant").toString());
            Long evenementId = Long.valueOf(body.get("evenementId").toString());
            Integer note = Integer.valueOf(body.get("note").toString());
            String commentaire = body.containsKey("commentaire") ? body.get("commentaire").toString() : null;

            FeedbackDto dto = feedbackService.createOrUpdateFeedback(idEtudiant, evenementId, note, commentaire);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/evenement/{evenementId}")
    public ResponseEntity<List<FeedbackDto>> getFeedbacksByEvenement(@PathVariable Long evenementId) {
        return ResponseEntity.ok(feedbackService.getFeedbacksByEvenement(evenementId));
    }

    @GetMapping("/moyenne/{evenementId}")
    public ResponseEntity<MoyenneFeedbackDto> getMoyenneByEvenement(@PathVariable Long evenementId) {
        return ResponseEntity.ok(feedbackService.getMoyenneByEvenement(evenementId));
    }

    @GetMapping("/admin/tous")
    public ResponseEntity<List<FeedbackDto>> getAllFeedbacksForAdmin() {
        return ResponseEntity.ok(feedbackService.getAllFeedbacksForAdmin());
    }

    @GetMapping("/verifier/{idEtudiant}/{evenementId}")
    public ResponseEntity<Boolean> hasEtudiantFeedback(@PathVariable Long idEtudiant, @PathVariable Long evenementId) {
        return ResponseEntity.ok(feedbackService.hasEtudiantFeedback(idEtudiant, evenementId));
    }
}

package org.example.club.controller;

import org.example.club.dto.FeedbackClubRequestDto;
import org.example.club.service.FeedbackClubService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs/feedback")
@CrossOrigin(origins = "*")
public class FeedbackClubController {

    private final FeedbackClubService feedbackService;

    public FeedbackClubController(FeedbackClubService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<?> createOrUpdate(@RequestBody FeedbackClubRequestDto request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.createOrUpdate(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /** Résumés d'avis par club (lecture publique pour la liste des clubs). */
    @GetMapping("/public/summaries")
    public ResponseEntity<?> getPublicSummaries() {
        return ResponseEntity.ok(feedbackService.getPublicSummaries());
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<?> getByClub(@PathVariable Long clubId) {
        return ResponseEntity.ok(feedbackService.getByClub(clubId));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllForAdmin() {
        return ResponseEntity.ok(feedbackService.getAll());
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<?> getStatsForAdmin() {
        return ResponseEntity.ok(feedbackService.getAdminStats());
    }
}


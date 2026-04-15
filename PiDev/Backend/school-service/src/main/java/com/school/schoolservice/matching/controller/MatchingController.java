package com.school.schoolservice.matching.controller;

import com.school.schoolservice.matching.dto.MatchingResultDto;
import com.school.schoolservice.matching.dto.VisitorProfileDto;
import com.school.schoolservice.matching.service.MatchingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MatchingController {

    private final MatchingService matchingService;

    // ✅ Matching pour visiteur anonyme
    @PostMapping("/visitor")
    public ResponseEntity<List<MatchingResultDto>> getMatchingForVisitor(
            @RequestBody VisitorProfileDto visitor) {
        return ResponseEntity.ok(matchingService.getMatchingOffersForVisitor(visitor));
    }

    // ✅ Score d'une offre spécifique
    @PostMapping("/visitor/offer/{offerId}")
    public ResponseEntity<MatchingResultDto> getMatchScore(
            @PathVariable Long offerId,
            @RequestBody VisitorProfileDto visitor) {
        return ResponseEntity.ok(matchingService.getMatchScore(offerId, visitor));
    }
}
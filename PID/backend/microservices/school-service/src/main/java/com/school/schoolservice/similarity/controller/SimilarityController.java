package com.school.schoolservice.similarity.controller;

import com.school.schoolservice.similarity.dto.SimilarityResultDto;
import com.school.schoolservice.similarity.service.SimilarityService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/similarity")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SimilarityController {

    private final SimilarityService similarityService;

    // ✅ TOP 3 offres similaires
    @GetMapping("/{offerId}")
    public ResponseEntity<List<SimilarityResultDto>> getSimilarOffers(
            @PathVariable Long offerId) {
        return ResponseEntity.ok(similarityService.findSimilarOffers(offerId, 3));
    }

    // ✅ TOP N offres similaires
    @GetMapping("/{offerId}/top/{n}")
    public ResponseEntity<List<SimilarityResultDto>> getTopNSimilarOffers(
            @PathVariable Long offerId,
            @PathVariable int n) {
        return ResponseEntity.ok(similarityService.findSimilarOffers(offerId, n));
    }
}
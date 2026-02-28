package com.school.schoolservice.matching.controller;

import com.school.schoolservice.matching.dto.MatchingResultDto;
import com.school.schoolservice.matching.entity.StudentProfile;
import com.school.schoolservice.matching.service.MatchingService;
import com.school.schoolservice.matching.service.StudentProfileService;
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
    private final StudentProfileService studentProfileService;

    @GetMapping("/{studentId}")
    public ResponseEntity<List<MatchingResultDto>> getMatchingOffers(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(matchingService.getMatchingOffers(studentId));
    }

    @GetMapping("/{studentId}/offer/{offerId}")
    public ResponseEntity<MatchingResultDto> getMatchScore(
            @PathVariable Long studentId,
            @PathVariable Long offerId) {
        return ResponseEntity.ok(matchingService.getMatchScore(studentId, offerId));
    }

    @PostMapping("/profile/{studentId}")
    public ResponseEntity<StudentProfile> saveProfile(
            @PathVariable Long studentId,
            @RequestBody StudentProfile profile) {
        return ResponseEntity.ok(studentProfileService.save(studentId, profile));
    }

    @GetMapping("/profile/{studentId}")
    public ResponseEntity<StudentProfile> getProfile(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(
                studentProfileService.findByStudentId(studentId)
                        .orElse(new StudentProfile())
        );
    }
}

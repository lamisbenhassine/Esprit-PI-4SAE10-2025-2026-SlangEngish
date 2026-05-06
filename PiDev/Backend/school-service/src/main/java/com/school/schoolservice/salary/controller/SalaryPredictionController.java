package com.school.schoolservice.salary.controller;

import com.school.schoolservice.salary.dto.SalaryPredictionDto;
import com.school.schoolservice.salary.service.SalaryPredictionService;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/salary")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SalaryPredictionController {

    private final SalaryPredictionService salaryPredictionService;
    private final JobOfferRepository jobOfferRepository;

    // ✅ Prédit le salaire depuis un titre manuel
    @GetMapping("/predict")
    public ResponseEntity<SalaryPredictionDto> predict(
            @RequestParam String jobTitle) {
        return ResponseEntity.ok(
                salaryPredictionService.predictSalary(jobTitle));
    }

    // ✅ Prédit le salaire depuis une offre existante
    @GetMapping("/predict/offer/{offerId}")
    public ResponseEntity<SalaryPredictionDto> predictFromOffer(
            @PathVariable Long offerId) {

        return jobOfferRepository.findById(offerId)
                .map(offer -> ResponseEntity.ok(
                        salaryPredictionService
                                .predictSalary(offer.getTitle())))
                .orElse(ResponseEntity.notFound().build());
    }
}
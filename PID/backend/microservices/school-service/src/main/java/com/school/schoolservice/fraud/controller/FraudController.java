package com.school.schoolservice.fraud.controller;

import com.school.schoolservice.fraud.dto.FraudResultDto;
import com.school.schoolservice.fraud.service.FraudDetectionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FraudController {

    private final FraudDetectionService fraudService;

    // ✅ Liste toutes les candidatures suspectes
    @GetMapping("/suspicious")
    public ResponseEntity<List<FraudResultDto>> getSuspicious() {
        return ResponseEntity.ok(fraudService.getAllSuspicious());
    }

    @GetMapping("/all")
    public ResponseEntity<List<FraudResultDto>> getAll() {
        return ResponseEntity.ok(fraudService.getAllFraud());
    }
}
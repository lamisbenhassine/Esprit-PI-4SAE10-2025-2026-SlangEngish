package com.school.schoolservice.fraud.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudResultDto {
    private Long applicationId;
    private Long studentId;
    private Long jobOfferId;
    private String applicantEmail;

    private double speedScore;
    private double emailScore;
    private double duplicateScore;
    private double volumeScore;
    private double totalScore;

    private String fraudLevel;      // CLEAN / SUSPICIOUS / BLOCKED
    private List<String> reasons;   // Raisons détectées
    private LocalDateTime detectedAt;

    // ✅ Helper
    public boolean isSuspicious() {
        return "SUSPICIOUS".equals(fraudLevel) || "BLOCKED".equals(fraudLevel);
    }
}
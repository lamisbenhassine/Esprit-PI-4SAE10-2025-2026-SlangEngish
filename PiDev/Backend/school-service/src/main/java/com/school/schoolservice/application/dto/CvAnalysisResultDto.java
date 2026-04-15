package com.school.schoolservice.application.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvAnalysisResultDto {
    private Long applicationId;
    private Long jobOfferId;
    private String applicantName;

    // ✅ Texte extrait du CV
    private String extractedText;

    // ✅ Données détectées
    private List<String> detectedSkills;
    private String detectedLocation;
    private int detectedExperienceYears;
    private String detectedEmail;

    // ✅ Scores matching
    private double skillsScore;
    private double locationScore;
    private double experienceScore;
    private double overallScore;
    private int overallPercent;
    private String matchLevel;
}
package com.school.schoolservice.application.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvScreeningResultDto {
    private Long applicationId;
    private String applicantName;
    private String applicantEmail;
    private String cvUrl;
    private int overallScore;
    private String matchLevel;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String detectedLocation;
    private int detectedExperienceYears;
    private double skillsScore;
    private double locationScore;
    private double experienceScore;
    private int rank;
}
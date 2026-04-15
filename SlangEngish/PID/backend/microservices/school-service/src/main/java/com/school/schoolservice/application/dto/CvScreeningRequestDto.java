package com.school.schoolservice.application.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvScreeningRequestDto {
    private Long jobOfferId;
    private List<String> requiredSkills;
    private String preferredLocation;
    private int minExperienceYears;
    private int topN; // nombre de candidats à retourner
}
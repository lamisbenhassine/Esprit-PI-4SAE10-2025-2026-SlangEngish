package com.school.schoolservice.matching.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingResultDto {
    private Long offerId;
    private String offerTitle;
    private String offerCompany;
    private String offerLocation;
    private String offerContractType;
    private String offerSalary;
    private double matchScore;
    private int matchPercent;
    private String matchLevel;
    private double locationScore;
    private double contractScore;
    private double salaryScore;
    private double keywordScore;
}
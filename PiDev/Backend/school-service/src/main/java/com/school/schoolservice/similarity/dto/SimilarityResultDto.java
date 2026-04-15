package com.school.schoolservice.similarity.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarityResultDto {
    private Long offerId;
    private String offerTitle;
    private String offerCompany;
    private String offerLocation;
    private String offerContractType;
    private String offerSalary;
    private Boolean offerActive;
    private double similarityScore;
    private int similarityPercent;
    private double cosinusScore;
    private double contractScore;
    private double locationScore;
    private double salaryScore;
}
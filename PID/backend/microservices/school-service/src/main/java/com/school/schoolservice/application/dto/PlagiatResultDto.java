package com.school.schoolservice.application.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlagiatResultDto {
    private Long applicationId1;
    private String applicantName1;
    private String applicantEmail1;

    private Long applicationId2;
    private String applicantName2;
    private String applicantEmail2;

    private double similarityScore;
    private int similarityPercent;
    private String plagiatLevel; // PLAGIAT, SUSPECT, OK
}
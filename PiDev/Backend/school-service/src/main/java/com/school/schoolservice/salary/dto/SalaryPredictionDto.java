package com.school.schoolservice.salary.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryPredictionDto {
    private String jobTitle;
    private Integer salaryMonthlyTND;
    private Integer salaryAnnualTND;
    private Integer salaryRangeLow;
    private Integer salaryRangeHigh;
    private Integer salaryUSD;
    private Integer confidence;
    private String currency;
    private String status;
    private String message;
}
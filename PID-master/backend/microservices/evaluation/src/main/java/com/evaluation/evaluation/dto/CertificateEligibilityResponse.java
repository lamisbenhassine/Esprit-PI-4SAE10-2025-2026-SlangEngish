package com.evaluation.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateEligibilityResponse {
    private boolean eligible;
    private int passedCount;
}

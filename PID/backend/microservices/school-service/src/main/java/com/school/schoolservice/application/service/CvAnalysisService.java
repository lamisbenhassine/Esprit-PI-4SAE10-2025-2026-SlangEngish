package com.school.schoolservice.application.service;

import com.school.schoolservice.application.dto.CvAnalysisResultDto;

public interface CvAnalysisService {
    CvAnalysisResultDto analyzeCV(Long applicationId);
}
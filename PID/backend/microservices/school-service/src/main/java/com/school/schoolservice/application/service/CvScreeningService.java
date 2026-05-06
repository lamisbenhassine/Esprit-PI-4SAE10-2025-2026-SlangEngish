package com.school.schoolservice.application.service;

import com.school.schoolservice.application.dto.CvScreeningRequestDto;
import com.school.schoolservice.application.dto.CvScreeningResultDto;
import java.util.List;

public interface CvScreeningService {
    List<CvScreeningResultDto> screenCVs(CvScreeningRequestDto request);
}
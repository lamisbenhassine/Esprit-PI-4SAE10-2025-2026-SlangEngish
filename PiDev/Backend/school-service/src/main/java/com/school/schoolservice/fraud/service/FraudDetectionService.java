package com.school.schoolservice.fraud.service;

import com.school.schoolservice.fraud.dto.FraudResultDto;
import com.school.schoolservice.application.entity.Application;

import java.util.List;

public interface FraudDetectionService {
    FraudResultDto analyze(Application application);
    java.util.List<FraudResultDto> getAllSuspicious();
    List<FraudResultDto> getAllFraud();
}
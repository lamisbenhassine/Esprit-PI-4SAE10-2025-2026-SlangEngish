package com.school.schoolservice.salary.service;

import com.school.schoolservice.salary.dto.SalaryPredictionDto;

public interface SalaryPredictionService {
    SalaryPredictionDto predictSalary(String jobTitle);
}
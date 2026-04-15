package com.school.schoolservice.simulation.service;

import com.school.schoolservice.simulation.dto.QuizQuestionDto;
import com.school.schoolservice.simulation.dto.QuizRequestDto;
import com.school.schoolservice.simulation.dto.QuizResultDto;
import java.util.List;

public interface QuizService {
    int analyzeCvOnly(String cvUrl, Long jobOfferId);
    List<QuizQuestionDto> generateQuiz(Long jobOfferId);
    QuizResultDto evaluateQuiz(QuizRequestDto request);
}
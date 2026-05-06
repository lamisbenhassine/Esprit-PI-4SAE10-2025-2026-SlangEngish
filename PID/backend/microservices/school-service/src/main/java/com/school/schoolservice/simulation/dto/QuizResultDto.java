package com.school.schoolservice.simulation.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultDto {
    private int cvScore;
    private int quizScore;
    private int globalScore;
    private String globalLevel;
    private int correctAnswers;
    private int totalQuestions;
    private List<QuestionResult> questionResults;
    private List<String> cvMatchedSkills;
    private List<String> cvMissingSkills;
    private boolean qualified;
    private String message;
    private LocalDateTime scheduledInterview;
    private List<String> tips;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResult {
        private int number;
        private String question;
        private String selectedAnswer;
        private String correctAnswer;
        private boolean correct;
        private String explanation;
    }
}
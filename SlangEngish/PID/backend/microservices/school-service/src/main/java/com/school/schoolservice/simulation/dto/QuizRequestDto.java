package com.school.schoolservice.simulation.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRequestDto {
    private Long jobOfferId;
    private String cvUrl;
    private String applicantEmail;
    private String applicantName;
    private List<QuizAnswer> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizAnswer {
        private int questionNumber;
        private String selectedAnswer;
        private String correctAnswer;
        private String question;
    }
}
package com.school.schoolservice.simulation.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionDto {
    private int number;
    private String question;
    private String category;
    private List<String> choices;
    private String correctAnswer;
    private String explanation;
}
package com.school.schoolservice.simulation.controller;

import com.school.schoolservice.simulation.dto.QuizQuestionDto;
import com.school.schoolservice.simulation.dto.QuizRequestDto;
import com.school.schoolservice.simulation.dto.QuizResultDto;
import com.school.schoolservice.simulation.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QuizController {

    private final QuizService quizService;

    // ✅ Génère 5 questions aléatoires
    @GetMapping("/questions/{jobOfferId}")
    public ResponseEntity<List<QuizQuestionDto>> getQuestions(
            @PathVariable Long jobOfferId) {
        return ResponseEntity.ok(
                quizService.generateQuiz(jobOfferId));
    }

    // ✅ Évalue le quiz + CV
    @PostMapping("/evaluate")
    public ResponseEntity<QuizResultDto> evaluate(
            @RequestBody QuizRequestDto request) {
        return ResponseEntity.ok(
                quizService.evaluateQuiz(request));
    }

    // ✅ Démarre le quiz avec le CV de la candidature
    @GetMapping("/start/{jobOfferId}")
    public ResponseEntity<Map<String, Object>> startQuiz(
            @PathVariable Long jobOfferId,
            @RequestParam String cvUrl,
            @RequestParam String applicantEmail) {

        Map<String, Object> response = new HashMap<>();

        List<QuizQuestionDto> questions =
                quizService.generateQuiz(jobOfferId);

        int cvScore = quizService.analyzeCvOnly(cvUrl, jobOfferId);

        response.put("questions", questions);
        response.put("cvScore", cvScore);
        response.put("cvUrl", cvUrl);
        response.put("jobOfferId", jobOfferId);
        response.put("applicantEmail", applicantEmail);

        System.out.println("🚀 Quiz démarré pour: " + applicantEmail
                + " | CV Score: " + cvScore + "%");

        return ResponseEntity.ok(response);
    }
}
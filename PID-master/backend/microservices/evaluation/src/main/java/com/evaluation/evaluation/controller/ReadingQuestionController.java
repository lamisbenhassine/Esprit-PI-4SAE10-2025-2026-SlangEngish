package com.evaluation.evaluation.controller;

import com.evaluation.evaluation.model.ReadingQuestion;
import com.evaluation.evaluation.service.ReadingQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reading-questions")
@RequiredArgsConstructor
public class ReadingQuestionController {

    private final ReadingQuestionService readingQuestionService;

    @PostMapping
    public ResponseEntity<ReadingQuestion> addQuestion(@RequestBody ReadingQuestion question) {
        ReadingQuestion created = readingQuestionService.addQuestionToEvaluation(question);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/evaluation/{evaluationId}")
    public ResponseEntity<List<ReadingQuestion>> getQuestionsByEvaluation(@PathVariable Long evaluationId) {
        return ResponseEntity.ok(readingQuestionService.getQuestionsByEvaluation(evaluationId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        readingQuestionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}

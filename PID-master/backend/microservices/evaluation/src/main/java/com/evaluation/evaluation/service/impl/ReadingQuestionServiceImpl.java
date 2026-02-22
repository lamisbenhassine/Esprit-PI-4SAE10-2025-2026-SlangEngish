package com.evaluation.evaluation.service.impl;

import com.evaluation.evaluation.exception.ResourceNotFoundException;
import com.evaluation.evaluation.model.Evaluation;
import com.evaluation.evaluation.model.ReadingQuestion;
import com.evaluation.evaluation.repository.EvaluationRepository;
import com.evaluation.evaluation.repository.ReadingQuestionRepository;
import com.evaluation.evaluation.service.ReadingQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadingQuestionServiceImpl implements ReadingQuestionService {

    private final ReadingQuestionRepository readingQuestionRepository;
    private final EvaluationRepository evaluationRepository;

    @Override
    public ReadingQuestion addQuestionToEvaluation(ReadingQuestion question) {
        Long evaluationId = question.getEvaluationId();
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found with id: " + evaluationId));

        question.setEvaluation(evaluation);
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());

        return readingQuestionRepository.save(question);
    }

    @Override
    public void deleteQuestion(Long questionId) {
        ReadingQuestion question = readingQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));
        readingQuestionRepository.delete(question);
    }

    @Override
    public List<ReadingQuestion> getQuestionsByEvaluation(Long evaluationId) {
        if (!evaluationRepository.existsById(evaluationId)) {
            throw new ResourceNotFoundException("Evaluation not found with id: " + evaluationId);
        }
        return readingQuestionRepository.findByEvaluationId(evaluationId);
    }
}

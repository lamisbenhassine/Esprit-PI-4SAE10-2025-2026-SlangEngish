package com.evaluation.evaluation.service.impl;

import com.evaluation.evaluation.exception.ResourceNotFoundException;
import com.evaluation.evaluation.model.Evaluation;
import com.evaluation.evaluation.repository.EvaluationRepository;
import com.evaluation.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;

    @Override
    public Evaluation createEvaluation(Evaluation evaluation) {
        evaluation.setCreatedAt(LocalDateTime.now());
        evaluation.setUpdatedAt(LocalDateTime.now());
        return evaluationRepository.save(evaluation);
    }

    @Override
    public List<Evaluation> getAllEvaluations() {
        return evaluationRepository.findAll();
    }

    @Override
    public Evaluation getEvaluationById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found with id: " + id));
    }

    @Override
    public Evaluation updateEvaluation(Long id, Evaluation evaluationDetails) {
        Evaluation evaluation = getEvaluationById(id);

        evaluation.setTitle(evaluationDetails.getTitle());
        evaluation.setImageUrl(evaluationDetails.getImageUrl());
        evaluation.setDateStart(evaluationDetails.getDateStart());
        evaluation.setDateEnd(evaluationDetails.getDateEnd());
        evaluation.setDurationMinutes(evaluationDetails.getDurationMinutes());
        evaluation.setNumberOfAttempts(evaluationDetails.getNumberOfAttempts());
        evaluation.setTotalScore(evaluationDetails.getTotalScore());

        evaluation.setUpdatedAt(LocalDateTime.now());
        return evaluationRepository.save(evaluation);
    }

    @Override
    public void deleteEvaluation(Long id) {
        Evaluation evaluation = getEvaluationById(id);
        evaluationRepository.delete(evaluation);
    }

    @Override
    public List<Evaluation> getAvailableEvaluationsForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Evaluation> allEvaluations = evaluationRepository.findAll();
        
        return allEvaluations.stream()
                .filter(eval -> {
                    // Check date range
                    if (eval.getDateStart() != null && now.isBefore(eval.getDateStart())) {
                        return false;
                    }
                    if (eval.getDateEnd() != null && now.isAfter(eval.getDateEnd())) {
                        return false;
                    }
                    return true;
                })
                .toList();
    }
}

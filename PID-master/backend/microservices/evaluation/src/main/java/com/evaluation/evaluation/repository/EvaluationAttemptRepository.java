package com.evaluation.evaluation.repository;

import com.evaluation.evaluation.model.EvaluationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationAttemptRepository extends JpaRepository<EvaluationAttempt, Long> {
    List<EvaluationAttempt> findByUserIdAndEvaluationId(Long userId, Long evaluationId);
    List<EvaluationAttempt> findByEvaluationId(Long evaluationId);
}

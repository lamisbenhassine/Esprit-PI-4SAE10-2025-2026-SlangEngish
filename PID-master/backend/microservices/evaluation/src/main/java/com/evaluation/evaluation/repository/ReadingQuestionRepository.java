package com.evaluation.evaluation.repository;

import com.evaluation.evaluation.model.ReadingQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadingQuestionRepository extends JpaRepository<ReadingQuestion, Long> {
    List<ReadingQuestion> findByEvaluationId(Long evaluationId);
}

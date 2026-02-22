package com.evaluation.evaluation.service;

import com.evaluation.evaluation.model.ReadingQuestion;
import java.util.List;

public interface ReadingQuestionService {
    ReadingQuestion addQuestionToEvaluation(ReadingQuestion question);

    void deleteQuestion(Long questionId);

    List<ReadingQuestion> getQuestionsByEvaluation(Long evaluationId);
}

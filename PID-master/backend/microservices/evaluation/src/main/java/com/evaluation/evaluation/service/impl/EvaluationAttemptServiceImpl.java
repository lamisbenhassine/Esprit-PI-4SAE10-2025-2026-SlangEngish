package com.evaluation.evaluation.service.impl;

import com.evaluation.evaluation.enums.AttemptStatus;
import com.evaluation.evaluation.exception.ResourceNotFoundException;
import com.evaluation.evaluation.model.Evaluation;
import com.evaluation.evaluation.model.EvaluationAttempt;
import com.evaluation.evaluation.model.Question;
import com.evaluation.evaluation.model.StudentAnswer;
import com.evaluation.evaluation.repository.EvaluationAttemptRepository;
import com.evaluation.evaluation.repository.EvaluationRepository;
import com.evaluation.evaluation.repository.FillBlankQuestionRepository;
import com.evaluation.evaluation.repository.OptionRepository;
import com.evaluation.evaluation.repository.QuestionRepository;
import com.evaluation.evaluation.repository.StudentAnswerRepository;
import com.evaluation.evaluation.service.EvaluationAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationAttemptServiceImpl implements EvaluationAttemptService {

    private final EvaluationAttemptRepository evaluationAttemptRepository;
    private final EvaluationRepository evaluationRepository;
    private final QuestionRepository questionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final OptionRepository optionRepository;
    private final FillBlankQuestionRepository fillBlankQuestionRepository;

    @Override
    public EvaluationAttempt startAttempt(Long evaluationId, Long userId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found with id: " + evaluationId));

        // Check if evaluation is available (date range)
        LocalDateTime now = LocalDateTime.now();
        if (evaluation.getDateStart() != null && now.isBefore(evaluation.getDateStart())) {
            throw new IllegalStateException("Evaluation has not started yet");
        }
        if (evaluation.getDateEnd() != null && now.isAfter(evaluation.getDateEnd())) {
            throw new IllegalStateException("Evaluation deadline has passed");
        }

        // Check if user has reached max attempts
        List<EvaluationAttempt> existingAttempts = evaluationAttemptRepository.findByUserIdAndEvaluationId(userId, evaluationId);
        int submittedAttempts = (int) existingAttempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED)
                .count();
        
        if (evaluation.getNumberOfAttempts() != null && submittedAttempts >= evaluation.getNumberOfAttempts()) {
            throw new IllegalStateException("Maximum number of attempts reached. You have used " + submittedAttempts + " out of " + evaluation.getNumberOfAttempts() + " attempts");
        }

        // Check if there's an in-progress attempt
        EvaluationAttempt inProgressAttempt = existingAttempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);

        if (inProgressAttempt != null) {
            return inProgressAttempt; // Return existing in-progress attempt
        }

        EvaluationAttempt attempt = new EvaluationAttempt();
        attempt.setEvaluation(evaluation);
        attempt.setUserId(userId);
        attempt.setStartTime(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setAttemptNumber(submittedAttempts + 1);
        attempt.setCreatedAt(LocalDateTime.now());
        attempt.setUpdatedAt(LocalDateTime.now());

        return evaluationAttemptRepository.save(attempt);
    }

    private int getAttemptCount(Long userId, Long evaluationId) {
        return evaluationAttemptRepository.findByUserIdAndEvaluationId(userId, evaluationId).size();
    }

    @Override
    public EvaluationAttempt submitAnswer(Long attemptId, StudentAnswer answer, Long questionId) {
        EvaluationAttempt attempt = getAttemptById(attemptId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        answer.setEvaluationAttempt(attempt);
        answer.setQuestion(question);
        answer.setCreatedAt(LocalDateTime.now());
        answer.setUpdatedAt(LocalDateTime.now());

        // Handle options linkage if provided
        if (answer.getSelectedOptions() != null) {
            // In a real scenario, we might want to validate that these options belong to
            // the question
            // For now, we assume the IDs are correct
        }

        studentAnswerRepository.save(answer);

        // Return refreshed attempt
        return attempt;
    }

    @Override
    public EvaluationAttempt finishAttempt(Long attemptId) {
        EvaluationAttempt attempt = getAttemptById(attemptId);
        
        // Load student answers for scoring
        List<StudentAnswer> answers = studentAnswerRepository.findByEvaluationAttemptId(attemptId);
        attempt.setStudentAnswers(answers);
        
        attempt.setEndTime(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setUpdatedAt(LocalDateTime.now());

        // Calculate score
        double totalScore = calculateScore(attempt);
        attempt.setScore(totalScore);

        return evaluationAttemptRepository.save(attempt);
    }

    private double calculateScore(EvaluationAttempt attempt) {
        double totalScore = 0.0;
        for (StudentAnswer answer : attempt.getStudentAnswers()) {
            Question question = answer.getQuestion();
            boolean isCorrect = false;

            switch (question.getQuestionType()) {
                case MCQ:
                    if (answer.getSelectedOptions() != null && answer.getSelectedOptions().size() == 1) {
                        // Check if the selected option is correct
                        // We need to fetch the option from DB or if it's eagerly loaded
                        // Here we assume getSelectedOptions has valid Option entities, but we should
                        // check 'isCorrect' flag
                        // However, StudentAnswer.selectedOptions might be lazy or just references.
                        // Let's assume we can check against Question's options or the Option entity
                        // itself.
                        // Simplest: Check if the single selected option has isCorrect=true
                        isCorrect = answer.getSelectedOptions().get(0).getIsCorrect();
                    }
                    break;
                case MSQ:
                    // For MSQ: All correct options must be selected AND no incorrect options selected
                    if (answer.getSelectedOptions() != null && !answer.getSelectedOptions().isEmpty()) {
                        // Get all correct options for this question
                        List<com.evaluation.evaluation.model.Option> allOptions = optionRepository.findByQuestionId(question.getId());
                        List<com.evaluation.evaluation.model.Option> correctOptions = allOptions.stream()
                                .filter(com.evaluation.evaluation.model.Option::getIsCorrect)
                                .toList();
                        
                        // Get selected option IDs
                        List<Long> selectedOptionIds = answer.getSelectedOptions().stream()
                                .map(com.evaluation.evaluation.model.Option::getId)
                                .toList();
                        
                        // Check: all correct options must be selected
                        boolean allCorrectSelected = correctOptions.stream()
                                .allMatch(opt -> selectedOptionIds.contains(opt.getId()));
                        
                        // Check: no incorrect options selected
                        boolean noIncorrectSelected = answer.getSelectedOptions().stream()
                                .allMatch(com.evaluation.evaluation.model.Option::getIsCorrect);
                        
                        isCorrect = allCorrectSelected && noIncorrectSelected && 
                                   selectedOptionIds.size() == correctOptions.size();
                    }
                    break;
                case FILL_BLANK:
                    // Load FillBlankQuestion with blanks (base Question from answer may not have blanks loaded)
                    if (answer.getTextAnswer() != null) {
                        String[] userWords = answer.getTextAnswer().split(",");
                        var fbqOpt = fillBlankQuestionRepository.findById(question.getId());
                        if (fbqOpt.isPresent()) {
                            com.evaluation.evaluation.model.FillBlankQuestion fbq = fbqOpt.get();
                            List<com.evaluation.evaluation.model.Blank> blanks = fbq.getBlanks();
                            if (blanks != null && !blanks.isEmpty() && blanks.size() == userWords.length) {
                                // Sort blanks by positionIndex so order matches the student's answer
                                List<com.evaluation.evaluation.model.Blank> sortedBlanks = blanks.stream()
                                        .sorted(Comparator.comparing(com.evaluation.evaluation.model.Blank::getPositionIndex))
                                        .toList();
                                isCorrect = true;
                                for (int i = 0; i < sortedBlanks.size(); i++) {
                                    String correct = sortedBlanks.get(i).getCorrectWord();
                                    String user = userWords[i].trim();
                                    if (correct == null || !correct.equalsIgnoreCase(user)) {
                                        isCorrect = false;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    break;
                default:
                    // Reading/Writing need manual grading usually
                    break;
            }

            if (isCorrect) {
                totalScore += question.getPoints();
                answer.setScoreAwarded(question.getPoints());
            } else {
                answer.setScoreAwarded(0.0);
            }
        }
        return totalScore;
    }

    @Override
    public EvaluationAttempt getAttemptById(Long attemptId) {
        return evaluationAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));
    }

    @Override
    public List<EvaluationAttempt> getAttemptsByUserAndEvaluation(Long userId, Long evaluationId) {
        return evaluationAttemptRepository.findByUserIdAndEvaluationId(userId, evaluationId);
    }

    @Override
    public List<EvaluationAttempt> getAttemptsByEvaluation(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found with id: " + evaluationId));
        return evaluationAttemptRepository.findByEvaluationId(evaluationId);
    }

    @Override
    public StudentAnswer updateAnswerScore(Long answerId, Double newScore) {
        StudentAnswer answer = studentAnswerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Student answer not found with id: " + answerId));
        
        answer.setScoreAwarded(newScore);
        answer.setUpdatedAt(LocalDateTime.now());
        studentAnswerRepository.save(answer);

        // Recalculate total score for the attempt (sum all answer scores)
        EvaluationAttempt attempt = answer.getEvaluationAttempt();
        double totalScore = attempt.getStudentAnswers().stream()
                .mapToDouble(a -> a.getScoreAwarded() != null ? a.getScoreAwarded() : 0.0)
                .sum();
        attempt.setScore(totalScore);
        attempt.setUpdatedAt(LocalDateTime.now());
        evaluationAttemptRepository.save(attempt);

        return answer;
    }
}

package com.evaluation.evaluation.model;

import com.evaluation.evaluation.enums.AttemptStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "evaluation_attempts", indexes = {
        @Index(name = "idx_attempt_user_id", columnList = "user_id"),
        @Index(name = "idx_attempt_evaluation_id", columnList = "evaluation_id")
})
public class EvaluationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status;

    private Integer attemptNumber;

    private Boolean autoSubmitted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Evaluation evaluation;

    @OneToMany(mappedBy = "evaluationAttempt", cascade = CascadeType.ALL)
    private List<StudentAnswer> studentAnswers = new ArrayList<>();
}

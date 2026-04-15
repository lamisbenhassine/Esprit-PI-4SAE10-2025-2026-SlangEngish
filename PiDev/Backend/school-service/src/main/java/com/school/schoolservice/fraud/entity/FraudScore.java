package com.school.schoolservice.fraud.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_scores")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FraudScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long jobOfferId;

    @Column
    private String applicantEmail;

    // ✅ Scores individuels
    @Column
    private Double speedScore;

    @Column
    private Double emailScore;

    @Column
    private Double duplicateScore;

    @Column
    private Double volumeScore;

    // ✅ Score total
    @Column(nullable = false)
    private Double totalScore;

    // ✅ Niveau : CLEAN, SUSPICIOUS, BLOCKED
    @Column(nullable = false)
    private String fraudLevel;

    // ✅ Raisons détectées
    @Column(columnDefinition = "TEXT")
    private String reasons;

    @Column
    private LocalDateTime detectedAt;
}

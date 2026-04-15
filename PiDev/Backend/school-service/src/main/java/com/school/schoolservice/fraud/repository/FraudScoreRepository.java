package com.school.schoolservice.fraud.repository;

import com.school.schoolservice.fraud.entity.FraudScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FraudScoreRepository extends JpaRepository<FraudScore, Long> {
    Optional<FraudScore> findByApplicationId(Long applicationId);
    List<FraudScore> findByStudentId(Long studentId);
    List<FraudScore> findByFraudLevelNot(String level);

    // ✅ Compte candidatures du même étudiant dans la dernière minute
    @Query("SELECT COUNT(f) FROM FraudScore f WHERE f.studentId = :studentId " +
            "AND f.detectedAt >= :since")
    long countRecentByStudentId(Long studentId, LocalDateTime since);

    // ✅ Compte candidatures du même étudiant aujourd'hui
    @Query("SELECT COUNT(f) FROM FraudScore f WHERE f.studentId = :studentId " +
            "AND f.detectedAt >= :startOfDay")
    long countTodayByStudentId(Long studentId, LocalDateTime startOfDay);

    // ✅ Vérifie doublon même étudiant même offre
    @Query("SELECT COUNT(f) FROM FraudScore f WHERE f.studentId = :studentId " +
            "AND f.jobOfferId = :jobOfferId")
    long countDuplicates(Long studentId, Long jobOfferId);

    // ✅ Liste fraudes suspectes et bloquées
    List<FraudScore> findByFraudLevelIn(List<String> levels);
}
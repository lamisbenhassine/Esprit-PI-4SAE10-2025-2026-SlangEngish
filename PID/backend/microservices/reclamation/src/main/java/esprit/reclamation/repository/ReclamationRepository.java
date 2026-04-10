package esprit.reclamation.repository;

import esprit.reclamation.entity.Reclamation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {

    @Query("SELECT r FROM Reclamation r ORDER BY COALESCE(r.priorityScore, 0) DESC, r.createdAt DESC")
    Page<Reclamation> findAllOrderByPriorityDesc(Pageable pageable);

    long countByStatut(String statut);

    @Query("SELECT COUNT(r) FROM Reclamation r WHERE r.statut IN ('EN_COURS', 'IN_PROGRESS')")
    long countInProgressStatuses();

    List<Reclamation> findAllByOrderByCreatedAtDesc();
    List<Reclamation> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Reclamation> findByStudentIdAndNotificationReadFalseAndReponseAdminIsNotNullOrderByCreatedAtDesc(Long studentId);
    Optional<Reclamation> findTopByStudentIdAndStudentReportedTrueOrderByReportedAtDesc(Long studentId);
    boolean existsByStudentIdAndStudentReportedTrueAndReportedAtAfter(Long studentId, LocalDateTime threshold);

    @Query("SELECT COUNT(DISTINCT r.studentId) FROM Reclamation r WHERE r.issueKey = :issueKey AND r.statut <> 'RESOLUE'")
    long countDistinctStudentsOpenByIssueKey(@Param("issueKey") String issueKey);

    @Query("SELECT r FROM Reclamation r WHERE r.issueKey = :issueKey AND r.statut <> 'RESOLUE'")
    List<Reclamation> findAllOpenByIssueKey(@Param("issueKey") String issueKey);
}

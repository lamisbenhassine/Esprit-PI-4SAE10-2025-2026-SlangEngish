package esprit.reclamation.repository;

import esprit.reclamation.entity.Reclamation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
    List<Reclamation> findAllByOrderByCreatedAtDesc();
    List<Reclamation> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Reclamation> findByStudentIdAndNotificationReadFalseAndReponseAdminIsNotNullOrderByCreatedAtDesc(Long studentId);
}

package esprit.reclamation.repository;

import esprit.reclamation.entity.StudentReclamationBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentReclamationBlockRepository extends JpaRepository<StudentReclamationBlock, Long> {
    Optional<StudentReclamationBlock> findByStudentId(Long studentId);
}

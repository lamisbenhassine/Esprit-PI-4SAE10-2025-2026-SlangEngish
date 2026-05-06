package tn.esprit.gestionclasses.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.gestionclasses.Entities.KanbanColumn;
import tn.esprit.gestionclasses.Entities.StudentKanbanTask;

import java.util.List;
import java.util.Optional;

public interface StudentKanbanTaskRepository extends JpaRepository<StudentKanbanTask, Long> {

    List<StudentKanbanTask> findByUserIdAndColumnStatus(Long userId, KanbanColumn columnStatus);

    List<StudentKanbanTask> findByUserId(Long userId);

    Optional<StudentKanbanTask> findByIdAndUserId(Long id, Long userId);
}

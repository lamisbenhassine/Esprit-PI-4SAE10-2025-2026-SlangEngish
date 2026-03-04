package tn.esprit.gestioncours.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.gestioncours.Entities.Progression;

import java.util.List;
import java.util.Optional;

public interface ProgressionRepository extends JpaRepository<Progression, Long> {

    Optional<Progression> findByUserIdAndChapter_IdChapter(Long userId, Long chapterId);

    List<Progression> findByUserIdAndChapter_Course_IdCourse(Long userId, Long courseId);
}


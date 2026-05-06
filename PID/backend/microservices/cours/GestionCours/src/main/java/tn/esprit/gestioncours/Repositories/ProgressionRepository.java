package tn.esprit.gestioncours.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.gestioncours.Entities.Progression;

import java.util.List;
import java.util.Optional;

public interface ProgressionRepository extends JpaRepository<Progression, Long> {

    Optional<Progression> findByUserIdAndChapter_IdChapter(Long userId, Long chapterId);

    List<Progression> findByUserIdAndChapter_Course_IdCourse(Long userId, Long courseId);
    Optional<Progression> findTopByUserIdOrderByIdDesc(Long userId);

    @Modifying
    @Transactional
    void deleteByChapter_Course_IdCourse(Long courseId);
}


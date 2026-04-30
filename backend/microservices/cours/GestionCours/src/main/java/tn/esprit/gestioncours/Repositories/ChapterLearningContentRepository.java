package tn.esprit.gestioncours.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.gestioncours.Entities.ChapterLearningContent;

import java.util.Optional;

public interface ChapterLearningContentRepository extends JpaRepository<ChapterLearningContent, Long> {

    Optional<ChapterLearningContent> findByChapter_IdChapter(Long idChapter);

    @Modifying
    @Transactional
    void deleteByChapter_Course_IdCourse(Long courseId);
}

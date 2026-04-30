package tn.esprit.gestioncours.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.gestioncours.Entities.ChapterPresentation;

import java.util.Optional;

public interface ChapterPresentationRepository extends JpaRepository<ChapterPresentation, Long> {

    Optional<ChapterPresentation> findByChapter_IdChapter(Long chapterId);

    @Modifying
    @Transactional
    void deleteByChapter_Course_IdCourse(Long courseId);
}

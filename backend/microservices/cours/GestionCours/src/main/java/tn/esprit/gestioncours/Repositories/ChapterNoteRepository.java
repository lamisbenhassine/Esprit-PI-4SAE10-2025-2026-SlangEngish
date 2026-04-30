package tn.esprit.gestioncours.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.gestioncours.Entities.ChapterNote;

import java.util.List;
import java.util.Optional;

public interface ChapterNoteRepository extends JpaRepository<ChapterNote, Long> {

    // Natif MySQL : évite LOWER() JPQL sur LONGTEXT (@Lob). blankFlag 1 = sans filtre recherche.
    @Query(
            value = "SELECT * FROM chapter_note n WHERE n.user_id = :userId AND n.chapter_id_chapter = :chapterId "
                    + "AND (:blankFlag = 1 OR LOWER(n.title) LIKE :pattern OR LOWER(COALESCE(n.content, '')) LIKE :pattern) "
                    + "ORDER BY n.updated_at DESC",
            nativeQuery = true)
    List<ChapterNote> findByUserAndChapterWithOptionalSearch(
            @Param("userId") Long userId,
            @Param("chapterId") Long chapterId,
            @Param("blankFlag") int blankFlag,
            @Param("pattern") String pattern
    );

    Optional<ChapterNote> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Transactional
    void deleteByChapter_Course_IdCourse(Long courseId);
}

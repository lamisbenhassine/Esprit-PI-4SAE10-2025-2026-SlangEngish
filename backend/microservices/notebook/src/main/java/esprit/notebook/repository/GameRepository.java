package esprit.notebook.repository;

import esprit.notebook.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByPublishedTrueOrderByCreatedAtDesc();

    List<Game> findByPublishedTrueAndCreatedAtAfterOrderByCreatedAtDesc(Instant after);

    List<Game> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);
}


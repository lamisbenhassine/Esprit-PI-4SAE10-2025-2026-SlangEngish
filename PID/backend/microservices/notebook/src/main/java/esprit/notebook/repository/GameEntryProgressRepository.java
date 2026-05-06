package esprit.notebook.repository;

import esprit.notebook.model.GameEntryProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameEntryProgressRepository extends JpaRepository<GameEntryProgress, Long> {
    Optional<GameEntryProgress> findByUserIdAndEntryId(Long userId, Long entryId);

    List<GameEntryProgress> findByUserIdAndGameId(Long userId, Long gameId);
}


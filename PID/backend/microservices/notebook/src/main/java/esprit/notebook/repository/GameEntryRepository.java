package esprit.notebook.repository;

import esprit.notebook.model.GameEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameEntryRepository extends JpaRepository<GameEntry, Long> {
    List<GameEntry> findByGameIdOrderByEntryOrderAsc(Long gameId);
}


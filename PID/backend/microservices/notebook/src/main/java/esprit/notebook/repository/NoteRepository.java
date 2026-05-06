package esprit.notebook.repository;

import esprit.notebook.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<Note> findBySharedTrueOrderBySharedAtDescUpdatedAtDesc();

    Optional<Note> findByShareId(String shareId);
}

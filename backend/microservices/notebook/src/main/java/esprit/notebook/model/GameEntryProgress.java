package esprit.notebook.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "notebook_game_entry_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_progress_user_entry", columnNames = {"user_id", "entry_id"})
        },
        indexes = {
                @Index(name = "idx_progress_user", columnList = "user_id"),
                @Index(name = "idx_progress_entry", columnList = "entry_id"),
                @Index(name = "idx_progress_game_user", columnList = "game_id, user_id")
        }
)
public class GameEntryProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    /** 0=no hint, 1..N increasing. */
    @Column(name = "hint_level", nullable = false)
    private int hintLevel = 0;

    @Column(name = "solved", nullable = false)
    private boolean solved = false;

    @Column(name = "solved_at")
    private Instant solvedAt;

    @Column(name = "last_answer", length = 60)
    private String lastAnswer;
}


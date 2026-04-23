package esprit.notebook.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "notebook_games",
        indexes = {
                @Index(name = "idx_game_teacher", columnList = "teacher_id"),
                @Index(name = "idx_game_published", columnList = "published"),
                @Index(name = "idx_game_created_at", columnList = "created_at")
        }
)
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String type = "CROSSWORD";

    @Column(nullable = false)
    private boolean published = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("entryOrder asc")
    private List<GameEntry> entries = new ArrayList<>();

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}


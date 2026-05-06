package esprit.notebook.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "notebook_notes",
        indexes = {
                @Index(name = "idx_note_user", columnList = "user_id"),
                @Index(name = "idx_note_shared", columnList = "shared"),
                @Index(name = "idx_note_share_id", columnList = "share_id")
        }
)
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    /** Higher = more “share-worthy” (length + readability heuristic). */
    @Column(name = "share_score")
    private Integer shareScore = 0;

    /** If true, this note appears in the shared space. */
    @Column(name = "shared", nullable = false)
    private boolean shared = false;

    /** Public share id (stable), used for importing a shared note. */
    @Column(name = "share_id", length = 40, unique = true)
    private String shareId = UUID.randomUUID().toString();

    @Column(name = "shared_at")
    private Instant sharedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}

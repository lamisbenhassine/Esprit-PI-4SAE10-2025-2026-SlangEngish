package esprit.forum.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "direct_conversation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_direct_participants",
                columnNames = {"participant_low_id", "participant_high_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectConversation {

    public enum DirectKind {
        /** Apprenant ↔ apprenant (aucun tuteur). */
        PEER,
        /** Au moins un participant est tuteur. */
        WITH_TUTOR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participant_low_id", nullable = false)
    private Long participantLowId;

    @Column(name = "participant_high_id", nullable = false)
    private Long participantHighId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DirectKind kind;

    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

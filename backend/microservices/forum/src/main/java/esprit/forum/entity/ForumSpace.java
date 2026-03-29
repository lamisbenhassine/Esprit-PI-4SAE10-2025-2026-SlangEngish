package esprit.forum.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "forum_space",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_forum_space_type_key", columnNames = {"type", "space_key"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumSpace {

    public enum ForumSpaceType {
        GENERAL,
        LEVEL,
        COURSE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ForumSpaceType type;

    @Column(name = "space_key", nullable = false, length = 100)
    private String key;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

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
        if (isPublic == null) {
            isPublic = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}


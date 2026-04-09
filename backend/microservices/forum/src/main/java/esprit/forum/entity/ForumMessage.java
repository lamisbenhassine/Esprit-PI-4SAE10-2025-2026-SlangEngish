package esprit.forum.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_message")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id")
    private Long authorId;

    @Column(length = 8000)
    private String content;

    /**
     * JSON: [{"type":"image","url":"..."},{"type":"video","url":"https://youtube.com/watch?v=..."}]
     */
    @Column(name = "attachments", columnDefinition = "TEXT")
    private String attachments;

    @Column(name = "parent_message_id")
    private Long parentMessageId; // null pour les posts, non-null pour les commentaires

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    @JsonIgnore
    private ForumTopic topic;

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

package esprit.forum.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forum_topic")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 4000)
    private String description;

    /** Image de couverture (URL absolue ou /forum-media/... après upload). */
    @Column(name = "cover_image_url", length = 2000)
    private String coverImageUrl;

    /** Vidéo du post (URL upload /forum-media/... ou lien externe). */
    @Column(name = "cover_video_url", length = 2000)
    private String coverVideoUrl;

    /** Mis en avant en tête de liste (modération backoffice). */
    @Column(name = "pinned")
    private Boolean pinned = Boolean.FALSE;

    /** Sujet verrouillé : plus de nouveaux messages (modération). */
    @Column(name = "locked")
    private Boolean locked = Boolean.FALSE;

    @Column(name = "author_id")
    private Long authorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private ForumSpace space;

    private String category; // GENERAL, A1, A2, B1, B2, C1, C2

    @Column(name = "is_public")
    private Boolean isPublic; // true pour forum général, false pour niveau spécifique

    private Integer views = 0;

    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ForumMessage> messages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (views == null) {
            views = 0;
        }
        if (pinned == null) {
            pinned = Boolean.FALSE;
        }
        if (locked == null) {
            locked = Boolean.FALSE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void incrementViews() {
        this.views++;
    }
}

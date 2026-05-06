package esprit.forum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedPostDto {
    private Long id;
    private String title;
    private String description;
    private Long authorId;
    /** Renseignés via le microservice user (Feign). */
    private String authorFirstName;
    private String authorLastName;
    /** STUDENT, TUTOR, ADMIN */
    private String authorRole;
    private String coverImageUrl;
    private String coverVideoUrl;
    private String category;
    private Integer views;

    /** GENERAL, LEVEL, COURSE — provenance du forum (fil = surtout général). */
    private String spaceType;
    private String spaceKey;
    private String spaceTitle;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private long likeCount;
    private boolean likedByViewer;
    private long commentCount;
    private long repostCount;
    private boolean repostedByViewer;

    /** Sujet verrouillé : pas de nouveaux commentaires. */
    private Boolean locked;
}

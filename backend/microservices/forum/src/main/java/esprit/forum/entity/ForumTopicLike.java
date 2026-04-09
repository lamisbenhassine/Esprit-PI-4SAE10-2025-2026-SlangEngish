package esprit.forum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "forum_topic_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "topic_id", "user_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumTopicLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}

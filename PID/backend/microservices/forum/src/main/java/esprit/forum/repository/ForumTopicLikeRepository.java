package esprit.forum.repository;

import esprit.forum.entity.ForumTopicLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ForumTopicLikeRepository extends JpaRepository<ForumTopicLike, Long> {

    long countByTopicId(Long topicId);

    boolean existsByTopicIdAndUserId(Long topicId, Long userId);

    Optional<ForumTopicLike> findByTopicIdAndUserId(Long topicId, Long userId);

    void deleteByTopicIdAndUserId(Long topicId, Long userId);
}

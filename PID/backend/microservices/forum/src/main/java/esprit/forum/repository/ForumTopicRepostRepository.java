package esprit.forum.repository;

import esprit.forum.entity.ForumTopicRepost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ForumTopicRepostRepository extends JpaRepository<ForumTopicRepost, Long> {

    long countByTopicId(Long topicId);

    boolean existsByTopicIdAndUserId(Long topicId, Long userId);

    Optional<ForumTopicRepost> findByTopicIdAndUserId(Long topicId, Long userId);
}

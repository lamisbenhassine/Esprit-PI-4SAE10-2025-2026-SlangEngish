package esprit.forum.repository;

import esprit.forum.entity.ForumTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumTopicRepository extends JpaRepository<ForumTopic, Long> {
    List<ForumTopic> findByIsPublicTrue();

    List<ForumTopic> findByCategoryAndIsPublicFalse(String category);

    List<ForumTopic> findByCategory(String category);

    List<ForumTopic> findByAuthorId(Long authorId);

    List<ForumTopic> findBySpaceId(Long spaceId);

    List<ForumTopic> findBySpaceIsNull();
}

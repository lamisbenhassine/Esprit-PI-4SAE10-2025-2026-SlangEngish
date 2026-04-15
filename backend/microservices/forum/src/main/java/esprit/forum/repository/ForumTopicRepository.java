package esprit.forum.repository;

import esprit.forum.entity.ForumTopic;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * Sujets sans aucun message dans le fil (description seule). Les plus anciens en premier
     * pour prioriser la file côté tuteurs / modération.
     */
    @Query(
            "SELECT t FROM ForumTopic t LEFT JOIN FETCH t.space "
                    + "WHERE NOT EXISTS (SELECT 1 FROM ForumMessage m WHERE m.topic.id = t.id) "
                    + "ORDER BY t.createdAt ASC")
    List<ForumTopic> findTopicsWithNoMessages(Pageable pageable);
}

package esprit.forum.repository;

import esprit.forum.entity.ForumMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumMessageRepository extends JpaRepository<ForumMessage, Long> {

    @EntityGraph(attributePaths = { "topic" })
    Page<ForumMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(m) FROM ForumMessage m WHERE m.topic.id = :topicId")
    long countMessagesForTopic(@Param("topicId") Long topicId);

    List<ForumMessage> findByTopicId(Long topicId);

    List<ForumMessage> findByParentMessageId(Long parentMessageId);

    List<ForumMessage> findByAuthorId(Long authorId);
}

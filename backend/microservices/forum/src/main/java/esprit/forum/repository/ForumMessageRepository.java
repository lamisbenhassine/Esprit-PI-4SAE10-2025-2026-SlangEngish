package esprit.forum.repository;

import esprit.forum.entity.ForumMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumMessageRepository extends JpaRepository<ForumMessage, Long> {
    List<ForumMessage> findByTopicId(Long topicId);

    List<ForumMessage> findByParentMessageId(Long parentMessageId);

    List<ForumMessage> findByAuthorId(Long authorId);
}

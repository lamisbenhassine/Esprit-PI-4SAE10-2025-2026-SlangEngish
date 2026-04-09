package esprit.forum.repository;

import esprit.forum.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    List<DirectMessage> findByConversation_IdOrderByCreatedAtAsc(Long conversationId);

    Optional<DirectMessage> findTopByConversation_IdOrderByCreatedAtDesc(Long conversationId);
}

package esprit.forum.repository;

import esprit.forum.entity.DirectConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DirectConversationRepository extends JpaRepository<DirectConversation, Long> {

    Optional<DirectConversation> findByParticipantLowIdAndParticipantHighId(Long participantLowId, Long participantHighId);

    @Query("SELECT c FROM DirectConversation c WHERE c.participantLowId = :uid OR c.participantHighId = :uid ORDER BY c.updatedAt DESC")
    List<DirectConversation> findForUserOrderByUpdated(@Param("uid") Long userId);
}

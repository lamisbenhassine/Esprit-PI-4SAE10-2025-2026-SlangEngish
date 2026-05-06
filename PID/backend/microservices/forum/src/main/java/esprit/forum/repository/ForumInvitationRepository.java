package esprit.forum.repository;

import esprit.forum.entity.ForumInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumInvitationRepository extends JpaRepository<ForumInvitation, Long> {

    List<ForumInvitation> findByToUserIdOrderByCreatedAtDesc(Long toUserId);

    List<ForumInvitation> findByFromUserIdOrderByCreatedAtDesc(Long fromUserId);
}

package esprit.forum.service;

import esprit.forum.entity.ForumInvitation;
import esprit.forum.repository.ForumInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumInvitationService {

    private final ForumInvitationRepository forumInvitationRepository;

    @Transactional
    public ForumInvitation sendInvitation(Long fromUserId, Long toUserId, String message) {
        if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Invitation invalide");
        }
        ForumInvitation inv = new ForumInvitation();
        inv.setFromUserId(fromUserId);
        inv.setToUserId(toUserId);
        String m = message != null ? message.strip() : "";
        if (m.length() > 500) {
            m = m.substring(0, 500);
        }
        inv.setMessage(m);
        inv.setStatus(ForumInvitation.Status.PENDING);
        return forumInvitationRepository.save(inv);
    }

    public List<ForumInvitation> inbox(Long userId) {
        return forumInvitationRepository.findByToUserIdOrderByCreatedAtDesc(userId);
    }

    public List<ForumInvitation> sent(Long userId) {
        return forumInvitationRepository.findByFromUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public ForumInvitation respond(Long invitationId, Long userId, boolean accept) {
        ForumInvitation inv = forumInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation introuvable"));
        if (!inv.getToUserId().equals(userId)) {
            throw new IllegalArgumentException("Non autorisé");
        }
        if (inv.getStatus() != ForumInvitation.Status.PENDING) {
            throw new IllegalArgumentException("Invitation déjà traitée");
        }
        inv.setStatus(accept ? ForumInvitation.Status.ACCEPTED : ForumInvitation.Status.DECLINED);
        return forumInvitationRepository.save(inv);
    }
}

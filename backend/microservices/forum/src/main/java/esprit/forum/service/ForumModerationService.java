package esprit.forum.service;

import esprit.forum.client.UserClient;
import esprit.forum.client.dto.UserProfileDto;
import esprit.forum.dto.ModerationMessageDto;
import esprit.forum.entity.ForumMessage;
import esprit.forum.repository.ForumMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumModerationService {

    private final ForumMessageRepository forumMessageRepository;
    private final ForumBlockService forumBlockService;
    private final UserClient userClient;

    @Transactional(readOnly = true)
    public List<ModerationMessageDto> listRecentMessages(Long moderatorUserId, int limit) {
        assertModerator(moderatorUserId);
        int cap = Math.min(Math.max(limit, 1), 200);
        return forumMessageRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, cap)).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMessageAsModerator(Long messageId, Long moderatorUserId) {
        assertModerator(moderatorUserId);
        deleteMessageRecursive(messageId);
    }

    @Transactional
    public void blockUserAsModerator(Long moderatorUserId, Long blockedUserId) {
        assertModerator(moderatorUserId);
        forumBlockService.blockUser(moderatorUserId, blockedUserId);
    }

    private void deleteMessageRecursive(Long messageId) {
        List<ForumMessage> children = forumMessageRepository.findByParentMessageId(messageId);
        for (ForumMessage c : children) {
            deleteMessageRecursive(c.getId());
        }
        forumMessageRepository.deleteById(messageId);
    }

    private void assertModerator(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("moderatorUserId requis");
        }
        UserProfileDto u = userClient.getUser(userId);
        if (u == null || u.getAccountRole() == null) {
            throw new IllegalArgumentException("Utilisateur introuvable");
        }
        String r = u.getAccountRole().trim().toUpperCase();
        if (!"TUTOR".equals(r) && !"ADMIN".equals(r)) {
            throw new IllegalArgumentException("Seuls les tuteurs ou administrateurs peuvent modérer les commentaires.");
        }
    }

    private ModerationMessageDto toDto(ForumMessage m) {
        ModerationMessageDto d = new ModerationMessageDto();
        d.setId(m.getId());
        d.setAuthorId(m.getAuthorId());
        d.setContent(m.getContent());
        d.setAttachments(m.getAttachments());
        d.setParentMessageId(m.getParentMessageId());
        d.setCreatedAt(m.getCreatedAt());
        if (m.getTopic() != null) {
            d.setTopicId(m.getTopic().getId());
            d.setTopicTitle(m.getTopic().getTitle());
        }
        return d;
    }
}

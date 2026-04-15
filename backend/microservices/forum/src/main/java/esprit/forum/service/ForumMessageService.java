package esprit.forum.service;

import esprit.forum.entity.ForumMessage;
import esprit.forum.entity.ForumTopic;
import esprit.forum.repository.ForumMessageRepository;
import esprit.forum.repository.ForumTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumMessageService {

    private final ForumMessageRepository forumMessageRepository;
    private final ForumTopicRepository forumTopicRepository;
    private final ForumBlockService forumBlockService;
    private final ContentModerationService contentModerationService;

    public List<ForumMessage> getMessagesByTopicId(Long topicId, Long viewerUserId) {
        List<ForumMessage> all = forumMessageRepository.findByTopicId(topicId);
        return filterByBlockedAuthors(all, viewerUserId);
    }

    public List<ForumMessage> getRepliesByParentId(Long parentMessageId, Long viewerUserId) {
        List<ForumMessage> all = forumMessageRepository.findByParentMessageId(parentMessageId);
        return filterByBlockedAuthors(all, viewerUserId);
    }

    private List<ForumMessage> filterByBlockedAuthors(List<ForumMessage> messages, Long viewerUserId) {
        Set<Long> blocked = forumBlockService.getBlockedUserIds(viewerUserId);
        return messages.stream()
                .filter(m -> m.getAuthorId() == null || !blocked.contains(m.getAuthorId()))
                .collect(Collectors.toList());
    }

    public List<ForumMessage> getMessagesByAuthor(Long authorId) {
        return forumMessageRepository.findByAuthorId(authorId);
    }

    public Optional<ForumMessage> getMessageById(Long id) {
        return forumMessageRepository.findById(id);
    }

    @Transactional
    public ForumMessage createMessage(ForumMessage message, Long topicId) {
        ForumTopic topic = forumTopicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found with id: " + topicId));
        if (Boolean.TRUE.equals(topic.getLocked())) {
            throw new RuntimeException("Topic is locked — no new messages");
        }
        message.setContent(contentModerationService.sanitizeText(message.getContent()));

        message.setTopic(topic);
        return forumMessageRepository.save(message);
    }

    @Transactional
    public ForumMessage updateMessage(Long id, ForumMessage updatedMessage) {
        return forumMessageRepository.findById(id)
                .map(message -> {
                    message.setContent(contentModerationService.sanitizeText(updatedMessage.getContent()));
                    if (updatedMessage.getAttachments() != null) {
                        message.setAttachments(updatedMessage.getAttachments());
                    }
                    return forumMessageRepository.save(message);
                })
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + id));
    }

    public void deleteMessage(Long id) {
        forumMessageRepository.deleteById(id);
    }
}

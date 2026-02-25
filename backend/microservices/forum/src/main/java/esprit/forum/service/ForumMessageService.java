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

@Service
@RequiredArgsConstructor
public class ForumMessageService {

    private final ForumMessageRepository forumMessageRepository;
    private final ForumTopicRepository forumTopicRepository;

    public List<ForumMessage> getMessagesByTopicId(Long topicId) {
        return forumMessageRepository.findByTopicId(topicId);
    }

    public List<ForumMessage> getRepliesByParentId(Long parentMessageId) {
        return forumMessageRepository.findByParentMessageId(parentMessageId);
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

        message.setTopic(topic);
        return forumMessageRepository.save(message);
    }

    @Transactional
    public ForumMessage updateMessage(Long id, ForumMessage updatedMessage) {
        return forumMessageRepository.findById(id)
                .map(message -> {
                    message.setContent(updatedMessage.getContent());
                    return forumMessageRepository.save(message);
                })
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + id));
    }

    public void deleteMessage(Long id) {
        forumMessageRepository.deleteById(id);
    }
}

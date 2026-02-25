package esprit.forum.service;

import esprit.forum.client.InscriptionClient;
import esprit.forum.entity.ForumTopic;
import esprit.forum.repository.ForumTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ForumTopicService {

    private final ForumTopicRepository forumTopicRepository;
    private final InscriptionClient inscriptionClient;

    public List<ForumTopic> getAllPublicTopics() {
        return forumTopicRepository.findByIsPublicTrue();
    }

    public List<ForumTopic> getTopicsByCategory(String category, Long userId) {
        // Check if accessing level-specific forum
        List<ForumTopic> topics = forumTopicRepository.findByCategory(category);

        // If category is not GENERAL and user is trying to access, verify payment
        if (!"GENERAL".equalsIgnoreCase(category)) {
            Boolean isPaid = inscriptionClient.isUserPaid(userId);
            if (!isPaid) {
                throw new RuntimeException("User must have an active subscription to access level-specific forums");
            }
        }

        return topics;
    }

    public Optional<ForumTopic> getTopicById(Long id) {
        return forumTopicRepository.findById(id);
    }

    @Transactional
    public ForumTopic incrementTopicViews(Long id) {
        ForumTopic topic = forumTopicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic not found with id: " + id));
        topic.incrementViews();
        return forumTopicRepository.save(topic);
    }

    public List<ForumTopic> getTopicsByAuthor(Long authorId) {
        return forumTopicRepository.findByAuthorId(authorId);
    }

    @Transactional
    public ForumTopic createTopic(ForumTopic topic) {
        // Validate category
        if ("GENERAL".equalsIgnoreCase(topic.getCategory())) {
            topic.setIsPublic(true);
        } else {
            topic.setIsPublic(false);
        }
        return forumTopicRepository.save(topic);
    }

    @Transactional
    public ForumTopic updateTopic(Long id, ForumTopic updatedTopic) {
        return forumTopicRepository.findById(id)
                .map(topic -> {
                    topic.setTitle(updatedTopic.getTitle());
                    topic.setDescription(updatedTopic.getDescription());
                    topic.setCategory(updatedTopic.getCategory());

                    // Update isPublic based on category
                    if ("GENERAL".equalsIgnoreCase(updatedTopic.getCategory())) {
                        topic.setIsPublic(true);
                    } else {
                        topic.setIsPublic(false);
                    }

                    return forumTopicRepository.save(topic);
                })
                .orElseThrow(() -> new RuntimeException("Topic not found with id: " + id));
    }

    public void deleteTopic(Long id) {
        forumTopicRepository.deleteById(id);
    }
}

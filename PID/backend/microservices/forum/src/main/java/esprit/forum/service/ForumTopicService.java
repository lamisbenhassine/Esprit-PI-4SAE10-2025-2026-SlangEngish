package esprit.forum.service;

import esprit.forum.entity.ForumSpace;
import esprit.forum.entity.ForumTopic;
import esprit.forum.repository.ForumSpaceRepository;
import esprit.forum.repository.ForumTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumTopicService {

    private final ForumTopicRepository forumTopicRepository;
    private final ForumSpaceRepository forumSpaceRepository;
    private final ForumBlockService forumBlockService;
    private final ContentModerationService contentModerationService;

    public List<ForumTopic> getAllPublicTopics(Long viewerUserId) {
        List<ForumTopic> topics = forumTopicRepository.findByIsPublicTrue();
        return filterAndSortTopics(topics, viewerUserId);
    }

    public List<ForumTopic> getTopicsByCategory(String category, Long userId) {
        List<ForumTopic> topics = forumTopicRepository.findByCategory(category);
        return filterAndSortTopics(topics, userId);
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

    public List<ForumTopic> getTopicsBySpaceId(Long spaceId, Long viewerUserId) {
        List<ForumTopic> topics = forumTopicRepository.findBySpaceId(spaceId);
        return filterAndSortTopics(topics, viewerUserId);
    }

    private List<ForumTopic> filterAndSortTopics(List<ForumTopic> topics, Long viewerUserId) {
        Set<Long> blocked = forumBlockService.getBlockedUserIds(viewerUserId);
        List<ForumTopic> filtered = topics.stream()
                .filter(t -> t.getAuthorId() == null || !blocked.contains(t.getAuthorId()))
                .collect(Collectors.toList());
        filtered.sort(Comparator
                .comparing((ForumTopic t) -> Boolean.TRUE.equals(t.getPinned()), Comparator.reverseOrder())
                .thenComparing(
                        t -> t.getUpdatedAt() != null ? t.getUpdatedAt() : LocalDateTime.MIN,
                        Comparator.reverseOrder()));
        return filtered;
    }

    @Transactional
    public ForumTopic createTopic(ForumTopic topic) {
        if (topic.getSpace() == null && topic.getCategory() != null && !topic.getCategory().trim().isEmpty()) {
            String normalized = topic.getCategory().trim().toUpperCase();
            ForumSpace space = "GENERAL".equals(normalized)
                    ? forumSpaceRepository.findByTypeAndKey(ForumSpace.ForumSpaceType.GENERAL, "GENERAL").orElse(null)
                    : forumSpaceRepository.findByTypeAndKey(ForumSpace.ForumSpaceType.LEVEL, normalized).orElse(null);
            topic.setSpace(space);
        }
        if ("GENERAL".equalsIgnoreCase(topic.getCategory())) {
            topic.setIsPublic(true);
        } else {
            topic.setIsPublic(false);
        }
        if (topic.getPinned() == null) {
            topic.setPinned(Boolean.FALSE);
        }
        if (topic.getLocked() == null) {
            topic.setLocked(Boolean.FALSE);
        }
        contentModerationService.assertTextAcceptable(topic.getTitle());
        contentModerationService.assertTextAcceptable(topic.getDescription());
        return forumTopicRepository.save(topic);
    }

    @Transactional
    public ForumTopic updateTopic(Long id, ForumTopic updatedTopic) {
        contentModerationService.assertTextAcceptable(updatedTopic.getTitle());
        contentModerationService.assertTextAcceptable(updatedTopic.getDescription());
        return forumTopicRepository.findById(id)
                .map(topic -> {
                    topic.setTitle(updatedTopic.getTitle());
                    topic.setDescription(updatedTopic.getDescription());
                    topic.setCategory(updatedTopic.getCategory());
                    if (updatedTopic.getCoverImageUrl() != null) {
                        topic.setCoverImageUrl(updatedTopic.getCoverImageUrl());
                    }
                    if (updatedTopic.getCoverVideoUrl() != null) {
                        topic.setCoverVideoUrl(updatedTopic.getCoverVideoUrl());
                    }
                    if (updatedTopic.getPinned() != null) {
                        topic.setPinned(updatedTopic.getPinned());
                    }
                    if (updatedTopic.getLocked() != null) {
                        topic.setLocked(updatedTopic.getLocked());
                    }

                    if ("GENERAL".equalsIgnoreCase(updatedTopic.getCategory())) {
                        topic.setIsPublic(true);
                    } else {
                        topic.setIsPublic(false);
                    }

                    return forumTopicRepository.save(topic);
                })
                .orElseThrow(() -> new RuntimeException("Topic not found with id: " + id));
    }

    /**
     * Supprime un sujet. Si {@code actorUserId} est renseigné, seul l’auteur du sujet peut le supprimer
     * (front-office). Sans {@code actorUserId}, suppression libre (back-office / admin).
     */
    public void deleteTopic(Long id, Long actorUserId) {
        ForumTopic t = forumTopicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found"));
        if (actorUserId != null) {
            if (t.getAuthorId() == null || !actorUserId.equals(t.getAuthorId())) {
                throw new IllegalStateException("You can only delete your own topics.");
            }
        }
        forumTopicRepository.deleteById(id);
    }

    @Transactional
    public ForumTopic moderateTopic(Long id, Boolean pinned, Boolean locked) {
        ForumTopic t = forumTopicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic not found with id: " + id));
        if (pinned != null) {
            t.setPinned(pinned);
        }
        if (locked != null) {
            t.setLocked(locked);
        }
        return forumTopicRepository.save(t);
    }

    /**
     * File « personne n’a encore répondu » : aucune ligne {@code forum_message} pour le sujet.
     * Filtre les auteurs bloqués par le lecteur (cohérent avec le reste du forum).
     */
    @Transactional(readOnly = true)
    public List<ForumTopic> listUnansweredTopics(int maxResults, Long viewerUserId) {
        int cap = Math.min(Math.max(maxResults, 1), 100);
        List<ForumTopic> raw = forumTopicRepository.findTopicsWithNoMessages(PageRequest.of(0, cap));
        Set<Long> blocked = forumBlockService.getBlockedUserIds(viewerUserId);
        return raw.stream()
                .filter(t -> t.getAuthorId() == null || !blocked.contains(t.getAuthorId()))
                .collect(Collectors.toList());
    }
}

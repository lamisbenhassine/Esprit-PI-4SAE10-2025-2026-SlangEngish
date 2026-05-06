package esprit.forum.service;

import esprit.forum.client.UserClient;
import esprit.forum.client.dto.UserProfileDto;
import esprit.forum.dto.FeedPostDto;
import esprit.forum.dto.LikeResponse;
import esprit.forum.dto.RepostResponse;
import esprit.forum.entity.ForumSpace;
import esprit.forum.entity.ForumTopic;
import esprit.forum.entity.ForumTopicLike;
import esprit.forum.entity.ForumTopicRepost;
import esprit.forum.repository.ForumMessageRepository;
import esprit.forum.repository.ForumTopicLikeRepository;
import esprit.forum.repository.ForumTopicRepostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForumFeedService {

    private final ForumTopicService forumTopicService;
    private final ForumTopicLikeRepository forumTopicLikeRepository;
    private final ForumTopicRepostRepository forumTopicRepostRepository;
    private final ForumMessageRepository forumMessageRepository;
    private final UserClient userClient;

    public List<FeedPostDto> getFeed(Long viewerUserId) {
        List<ForumTopic> topics = forumTopicService.getAllPublicTopics(viewerUserId);
        Map<Long, UserProfileDto> authors = loadAuthors(topics);
        return topics.stream()
                .map(t -> toDto(t, viewerUserId, authors.get(t.getAuthorId())))
                .collect(Collectors.toList());
    }

    private Map<Long, UserProfileDto> loadAuthors(List<ForumTopic> topics) {
        Set<Long> ids = new HashSet<>();
        for (ForumTopic t : topics) {
            if (t.getAuthorId() != null) {
                ids.add(t.getAuthorId());
            }
        }
        Map<Long, UserProfileDto> map = new HashMap<>();
        for (Long id : ids) {
            try {
                UserProfileDto u = userClient.getUser(id);
                if (u != null && u.getId() != null) {
                    map.put(u.getId(), u);
                }
            } catch (Exception e) {
                log.debug("User {} introuvable via user-service: {}", id, e.getMessage());
            }
        }
        return map;
    }

    private FeedPostDto toDto(ForumTopic t, Long viewerUserId, UserProfileDto author) {
        Long id = t.getId();
        FeedPostDto d = new FeedPostDto();
        d.setId(id);
        d.setTitle(t.getTitle());
        d.setDescription(t.getDescription());
        d.setAuthorId(t.getAuthorId());
        if (author != null) {
            d.setAuthorFirstName(author.getFirstName());
            d.setAuthorLastName(author.getLastName());
            d.setAuthorRole(author.getAccountRole());
        }
        d.setCoverImageUrl(t.getCoverImageUrl());
        d.setCoverVideoUrl(t.getCoverVideoUrl());
        d.setCategory(t.getCategory());
        d.setViews(t.getViews());
        ForumSpace s = t.getSpace();
        if (s != null) {
            d.setSpaceType(s.getType() != null ? s.getType().name() : null);
            d.setSpaceKey(s.getKey());
            d.setSpaceTitle(s.getTitle());
        }
        d.setCreatedAt(t.getCreatedAt());
        d.setLikeCount(forumTopicLikeRepository.countByTopicId(id));
        d.setLikedByViewer(viewerUserId != null
                && forumTopicLikeRepository.existsByTopicIdAndUserId(id, viewerUserId));
        d.setCommentCount(forumMessageRepository.countMessagesForTopic(id));
        d.setRepostCount(forumTopicRepostRepository.countByTopicId(id));
        d.setRepostedByViewer(viewerUserId != null
                && forumTopicRepostRepository.existsByTopicIdAndUserId(id, viewerUserId));
        d.setLocked(t.getLocked());
        return d;
    }

    @Transactional
    public LikeResponse toggleLike(Long topicId, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId required");
        }
        if (forumTopicLikeRepository.findByTopicIdAndUserId(topicId, userId).isPresent()) {
            forumTopicLikeRepository.deleteByTopicIdAndUserId(topicId, userId);
        } else {
            ForumTopicLike l = new ForumTopicLike();
            l.setTopicId(topicId);
            l.setUserId(userId);
            forumTopicLikeRepository.save(l);
        }
        long count = forumTopicLikeRepository.countByTopicId(topicId);
        boolean liked = forumTopicLikeRepository.existsByTopicIdAndUserId(topicId, userId);
        return new LikeResponse(liked, count);
    }

    @Transactional
    public RepostResponse toggleRepost(Long topicId, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId required");
        }
        if (forumTopicRepostRepository.findByTopicIdAndUserId(topicId, userId).isPresent()) {
            forumTopicRepostRepository.findByTopicIdAndUserId(topicId, userId)
                    .ifPresent(forumTopicRepostRepository::delete);
        } else {
            ForumTopicRepost r = new ForumTopicRepost();
            r.setTopicId(topicId);
            r.setUserId(userId);
            forumTopicRepostRepository.save(r);
        }
        long count = forumTopicRepostRepository.countByTopicId(topicId);
        boolean reposted = forumTopicRepostRepository.existsByTopicIdAndUserId(topicId, userId);
        return new RepostResponse(reposted, count);
    }
}

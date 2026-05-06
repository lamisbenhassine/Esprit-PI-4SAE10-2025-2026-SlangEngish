package esprit.forum.controller;

import esprit.forum.dto.FeedPostDto;
import esprit.forum.dto.LikeResponse;
import esprit.forum.dto.RepostResponse;
import esprit.forum.service.ForumFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum/feed")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumFeedController {

    private final ForumFeedService forumFeedService;

    @GetMapping
    public ResponseEntity<List<FeedPostDto>> feed(@RequestParam(required = false) Long viewerUserId) {
        return ResponseEntity.ok(forumFeedService.getFeed(viewerUserId));
    }

    @PostMapping("/topics/{topicId}/like")
    public ResponseEntity<?> like(@PathVariable Long topicId, @RequestParam Long userId) {
        try {
            LikeResponse r = forumFeedService.toggleLike(topicId, userId);
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/topics/{topicId}/repost")
    public ResponseEntity<?> repost(@PathVariable Long topicId, @RequestParam Long userId) {
        try {
            RepostResponse r = forumFeedService.toggleRepost(topicId, userId);
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

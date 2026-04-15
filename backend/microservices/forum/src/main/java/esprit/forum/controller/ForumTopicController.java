package esprit.forum.controller;

import esprit.forum.entity.ForumTopic;
import esprit.forum.service.ForumTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum/topics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumTopicController {

    private final ForumTopicService forumTopicService;

    @GetMapping("/general")
    public ResponseEntity<List<ForumTopic>> getGeneralTopics(
            @RequestParam(required = false) Long viewerUserId) {
        return ResponseEntity.ok(forumTopicService.getAllPublicTopics(viewerUserId));
    }

    /**
     * Sujets sans aucun message (file tuteurs / équipe). Déclaré avant {@code /{id}} pour éviter
     * la capture du segment « unanswered » comme identifiant.
     */
    @GetMapping("/unanswered")
    public ResponseEntity<List<ForumTopic>> listUnansweredTopics(
            @RequestParam(defaultValue = "40") int limit,
            @RequestParam(required = false) Long viewerUserId) {
        return ResponseEntity.ok(forumTopicService.listUnansweredTopics(limit, viewerUserId));
    }

    @GetMapping("/level/{category}")
    public ResponseEntity<?> getTopicsByLevel(@PathVariable String category,
            @RequestParam Long userId) {
        try {
            List<ForumTopic> topics = forumTopicService.getTopicsByCategory(category, userId);
            return ResponseEntity.ok(topics);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForumTopic> getTopicById(@PathVariable Long id) {
        ForumTopic topic = forumTopicService.getTopicById(id)
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        // Increment view count
        forumTopicService.incrementTopicViews(id);

        return ResponseEntity.ok(topic);
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<ForumTopic>> getTopicsByAuthor(@PathVariable Long authorId) {
        return ResponseEntity.ok(forumTopicService.getTopicsByAuthor(authorId));
    }

    @PostMapping
    public ResponseEntity<?> createTopic(@RequestBody ForumTopic topic) {
        try {
            ForumTopic createdTopic = forumTopicService.createTopic(topic);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTopic);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTopic(@PathVariable Long id,
            @RequestBody ForumTopic topic) {
        try {
            ForumTopic updatedTopic = forumTopicService.updateTopic(id, topic);
            return ResponseEntity.ok(updatedTopic);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTopic(
            @PathVariable Long id,
            @RequestParam(required = false) Long actorUserId) {
        try {
            forumTopicService.deleteTopic(id, actorUserId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /** Épinglage / verrouillage (backoffice modération). */
    @PatchMapping("/{id}/moderation")
    public ResponseEntity<?> moderateTopic(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(required = false) Boolean locked) {
        try {
            return ResponseEntity.ok(forumTopicService.moderateTopic(id, pinned, locked));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

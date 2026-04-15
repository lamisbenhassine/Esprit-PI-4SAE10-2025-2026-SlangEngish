package esprit.forum.controller;

import esprit.forum.entity.ForumSpace;
import esprit.forum.entity.ForumTopic;
import esprit.forum.service.ForumAccessService;
import esprit.forum.service.ForumSpaceService;
import esprit.forum.service.ForumTopicService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum/spaces")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumSpaceController {

    private final ForumSpaceService forumSpaceService;
    private final ForumAccessService forumAccessService;
    private final ForumTopicService forumTopicService;

    @GetMapping
    public ResponseEntity<List<ForumSpace>> listSpaces(@RequestParam ForumSpace.ForumSpaceType type) {
        return ResponseEntity.ok(forumSpaceService.getSpacesByType(type));
    }

    @GetMapping("/general")
    public ResponseEntity<ForumSpace> getGeneralSpace() {
        return forumSpaceService.findByTypeAndKey(ForumSpace.ForumSpaceType.GENERAL, "GENERAL")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<?> getLevelSpace(@PathVariable String level, @RequestParam Long userId) {
        try {
            ForumSpace space = forumSpaceService.findByTypeAndKey(ForumSpace.ForumSpaceType.LEVEL, level.toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Level space not found"));
            return ResponseEntity.ok(space);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: " + e.getMessage());
        }
    }

    @GetMapping("/course/{courseKey}")
    public ResponseEntity<?> getCourseSpace(@PathVariable String courseKey, @RequestParam Long userId) {
        try {
            ForumSpace space = forumSpaceService.findByTypeAndKey(ForumSpace.ForumSpaceType.COURSE, courseKey)
                    .orElseGet(() -> forumSpaceService.createIfMissing(
                            ForumSpace.ForumSpaceType.COURSE,
                            courseKey,
                            "Cours " + courseKey,
                            false
                    ));
            forumAccessService.assertCanAccess(space, userId);
            return ResponseEntity.ok(space);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: " + e.getMessage());
        }
    }

    @GetMapping("/{spaceId}/topics")
    public ResponseEntity<?> getTopicsBySpace(@PathVariable Long spaceId, @RequestParam(required = false) Long userId) {
        try {
            ForumSpace space = forumSpaceService.getSpaceById(spaceId)
                    .orElseThrow(() -> new RuntimeException("Forum space not found"));
            forumAccessService.assertCanAccess(space, userId);
            return ResponseEntity.ok(forumTopicService.getTopicsBySpaceId(spaceId, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: " + e.getMessage());
        }
    }

    @PostMapping("/{spaceId}/topics")
    public ResponseEntity<?> createTopicInSpace(@PathVariable Long spaceId, @RequestBody CreateTopicRequest request) {
        try {
            ForumSpace space = forumSpaceService.getSpaceById(spaceId)
                    .orElseThrow(() -> new RuntimeException("Forum space not found"));
            forumAccessService.assertCanAccess(space, request.getUserId());

            ForumTopic topic = new ForumTopic();
            topic.setTitle(request.getTitle());
            topic.setDescription(request.getDescription());
            topic.setAuthorId(request.getAuthorId());
            topic.setCoverImageUrl(request.getCoverImageUrl());
            topic.setCoverVideoUrl(request.getCoverVideoUrl());
            topic.setSpace(space);

            if (space.getType() == ForumSpace.ForumSpaceType.GENERAL) {
                topic.setCategory("GENERAL");
                topic.setIsPublic(true);
            } else if (space.getType() == ForumSpace.ForumSpaceType.LEVEL) {
                topic.setCategory(space.getKey());
                topic.setIsPublic(false);
            } else {
                topic.setCategory("COURSE");
                topic.setIsPublic(false);
            }

            ForumTopic created = forumTopicService.createTopic(topic);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Data
    public static class CreateTopicRequest {
        private Long userId;
        private Long authorId;
        private String title;
        private String description;
        /** URL image ou média servi par /forum-media/ après upload */
        private String coverImageUrl;
        private String coverVideoUrl;
    }
}


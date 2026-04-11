package esprit.forum.controller;

import esprit.forum.dto.ModerationMessageDto;
import esprit.forum.service.ForumModerationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Modération des commentaires (forum) : réservé aux tuteurs / administrateurs (contrôle via user-service).
 */
@RestController
@RequestMapping("/api/forum/moderation")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumModerationController {

    private final ForumModerationService forumModerationService;

    @GetMapping("/messages")
    public ResponseEntity<?> listRecent(
            @RequestParam Long moderatorUserId,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<ModerationMessageDto> list = forumModerationService.listRecentMessages(moderatorUserId, limit);
            return ResponseEntity.ok(list);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<?> delete(
            @PathVariable Long messageId,
            @RequestParam Long moderatorUserId) {
        try {
            forumModerationService.deleteMessageAsModerator(messageId, moderatorUserId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/block")
    public ResponseEntity<?> block(@RequestBody ModerationBlockRequest body) {
        try {
            forumModerationService.blockUserAsModerator(body.getModeratorUserId(), body.getBlockedUserId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Data
    public static class ModerationBlockRequest {
        private Long moderatorUserId;
        private Long blockedUserId;
    }
}

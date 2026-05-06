package esprit.forum.controller;

import esprit.forum.dto.ModerationMessageDto;
import esprit.forum.entity.ForumMessage;
import esprit.forum.service.ForumMessageService;
import esprit.forum.service.ForumModerationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumMessageController {

    private final ForumMessageService forumMessageService;
    private final ForumModerationService forumModerationService;

    @Deprecated
    @GetMapping("/topic/{topicId}")
    public ResponseEntity<List<ForumMessage>> getMessagesByTopic(@PathVariable Long topicId) {
        return ResponseEntity.ok(forumMessageService.getMessagesByTopicId(topicId, null));
    }

    @GetMapping("/replies/{parentMessageId}")
    public ResponseEntity<List<ForumMessage>> getReplies(
            @PathVariable Long parentMessageId,
            @RequestParam(required = false) Long viewerUserId) {
        return ResponseEntity.ok(forumMessageService.getRepliesByParentId(parentMessageId, viewerUserId));
    }

    /**
     * Liste des commentaires pour modération (tuteur / admin). Chemin stable sous ce contrôleur
     * (évite un 404 si un ancien déploiement n’expose pas {@code /api/forum/moderation/...}).
     */
    @GetMapping("/moderation/list")
    public ResponseEntity<?> listModeration(
            @RequestParam Long moderatorUserId,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<ModerationMessageDto> list = forumModerationService.listRecentMessages(moderatorUserId, limit);
            return ResponseEntity.ok(list);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/moderation/{messageId}")
    public ResponseEntity<?> deleteAsModerator(
            @PathVariable Long messageId,
            @RequestParam Long moderatorUserId) {
        try {
            forumModerationService.deleteMessageAsModerator(messageId, moderatorUserId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/moderation/block")
    public ResponseEntity<?> blockAsModerator(@RequestBody(required = false) ModerationBlockBody body) {
        try {
            if (body == null || body.getModeratorUserId() == null || body.getBlockedUserId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Corps JSON attendu : { \"moderatorUserId\": number, \"blockedUserId\": number }"));
            }
            forumModerationService.blockUserAsModerator(body.getModeratorUserId(), body.getBlockedUserId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<ForumMessage>> getMessagesByAuthor(@PathVariable Long authorId) {
        return ResponseEntity.ok(forumMessageService.getMessagesByAuthor(authorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForumMessage> getMessageById(@PathVariable Long id) {
        return forumMessageService.getMessageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Deprecated
    @PostMapping
    public ResponseEntity<ForumMessage> createMessage(@RequestBody CreateMessageRequest request) {
        try {
            ForumMessage message = new ForumMessage();
            message.setAuthorId(request.getAuthorId());
            message.setContent(request.getContent());
            message.setParentMessageId(request.getParentMessageId());

            ForumMessage createdMessage = forumMessageService.createMessage(message, request.getTopicId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdMessage);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMessage(@PathVariable Long id,
            @RequestBody UpdateMessageRequest request) {
        try {
            ForumMessage message = new ForumMessage();
            message.setContent(request.getContent());
            message.setAttachments(request.getAttachments());

            ForumMessage updatedMessage = forumMessageService.updateMessage(id, message);
            return ResponseEntity.ok(updatedMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        forumMessageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CreateMessageRequest {
        private Long topicId;
        private Long authorId;
        private String content;
        private Long parentMessageId; // null pour post, non-null pour commentaire
    }

    @Data
    public static class UpdateMessageRequest {
        private String content;
        private String attachments;
    }

    @Data
    public static class ModerationBlockBody {
        private Long moderatorUserId;
        private Long blockedUserId;
    }
}

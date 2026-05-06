package esprit.forum.controller;

import esprit.forum.entity.ForumMessage;
import esprit.forum.service.ForumMessageService;
import lombok.Data;
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
public class TopicMessageController {

    private final ForumMessageService forumMessageService;

    @GetMapping("/{topicId}/messages")
    public ResponseEntity<List<ForumMessage>> getMessagesByTopic(
            @PathVariable Long topicId,
            @RequestParam(required = false) Long viewerUserId) {
        return ResponseEntity.ok(forumMessageService.getMessagesByTopicId(topicId, viewerUserId));
    }

    @PostMapping("/{topicId}/messages")
    public ResponseEntity<?> createMessage(@PathVariable Long topicId, @RequestBody CreateMessageRequest request) {
        try {
            ForumMessage message = new ForumMessage();
            message.setAuthorId(request.getAuthorId());
            message.setContent(request.getContent());
            message.setParentMessageId(request.getParentMessageId());
            message.setAttachments(request.getAttachments());

            ForumMessage createdMessage = forumMessageService.createMessage(message, topicId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @Data
    public static class CreateMessageRequest {
        private Long authorId;
        private String content;
        private Long parentMessageId;
        /** JSON string attachments */
        private String attachments;
    }
}

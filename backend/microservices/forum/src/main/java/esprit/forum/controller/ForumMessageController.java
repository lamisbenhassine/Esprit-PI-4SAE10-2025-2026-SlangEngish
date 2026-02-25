package esprit.forum.controller;

import esprit.forum.entity.ForumMessage;
import esprit.forum.service.ForumMessageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumMessageController {

    private final ForumMessageService forumMessageService;

    @GetMapping("/topic/{topicId}")
    public ResponseEntity<List<ForumMessage>> getMessagesByTopic(@PathVariable Long topicId) {
        return ResponseEntity.ok(forumMessageService.getMessagesByTopicId(topicId));
    }

    @GetMapping("/replies/{parentMessageId}")
    public ResponseEntity<List<ForumMessage>> getReplies(@PathVariable Long parentMessageId) {
        return ResponseEntity.ok(forumMessageService.getRepliesByParentId(parentMessageId));
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
    public ResponseEntity<ForumMessage> updateMessage(@PathVariable Long id,
            @RequestBody UpdateMessageRequest request) {
        try {
            ForumMessage message = new ForumMessage();
            message.setContent(request.getContent());

            ForumMessage updatedMessage = forumMessageService.updateMessage(id, message);
            return ResponseEntity.ok(updatedMessage);
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
    }
}

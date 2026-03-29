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
@RequestMapping("/api/forum/topics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TopicMessageController {

    private final ForumMessageService forumMessageService;

    @GetMapping("/{topicId}/messages")
    public ResponseEntity<List<ForumMessage>> getMessagesByTopic(@PathVariable Long topicId) {
        return ResponseEntity.ok(forumMessageService.getMessagesByTopicId(topicId));
    }

    @PostMapping("/{topicId}/messages")
    public ResponseEntity<ForumMessage> createMessage(@PathVariable Long topicId, @RequestBody CreateMessageRequest request) {
        ForumMessage message = new ForumMessage();
        message.setAuthorId(request.getAuthorId());
        message.setContent(request.getContent());
        message.setParentMessageId(request.getParentMessageId());

        ForumMessage createdMessage = forumMessageService.createMessage(message, topicId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMessage);
    }

    @Data
    public static class CreateMessageRequest {
        private Long authorId;
        private String content;
        private Long parentMessageId;
    }
}


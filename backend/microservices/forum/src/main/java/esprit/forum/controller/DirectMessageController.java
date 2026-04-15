package esprit.forum.controller;

import esprit.forum.dto.DirectConversationSummaryDto;
import esprit.forum.dto.OpenDirectConversationRequest;
import esprit.forum.dto.SendDirectMessageRequest;
import esprit.forum.entity.DirectConversation;
import esprit.forum.entity.DirectMessage;
import esprit.forum.service.DirectConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum/direct")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DirectMessageController {

    private final DirectConversationService directConversationService;

    @PostMapping("/open")
    public ResponseEntity<?> open(@RequestBody OpenDirectConversationRequest body) {
        try {
            DirectConversation c = directConversationService.openOrGet(
                    new DirectConversationService.OpenParams(body.getUserId(), body.getWithUserId(), body.getKind()));
            return ResponseEntity.ok(c);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Service utilisateur indisponible ou erreur réseau");
        }
    }

    @GetMapping("/inbox")
    public ResponseEntity<?> inbox(@RequestParam Long userId) {
        try {
            List<DirectConversationSummaryDto> list = directConversationService.inbox(userId);
            return ResponseEntity.ok(list);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<?> listMessages(
            @PathVariable Long conversationId,
            @RequestParam Long userId) {
        try {
            List<DirectMessage> msgs = directConversationService.messages(conversationId, userId);
            return ResponseEntity.ok(msgs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<?> send(
            @PathVariable Long conversationId,
            @RequestBody SendDirectMessageRequest body) {
        try {
            DirectMessage m = directConversationService.send(conversationId, body.getSenderId(), body.getContent());
            return ResponseEntity.status(HttpStatus.CREATED).body(m);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}

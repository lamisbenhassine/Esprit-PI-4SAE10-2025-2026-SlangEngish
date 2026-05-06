package esprit.forum.controller;

import esprit.forum.entity.ForumInvitation;
import esprit.forum.service.ForumInvitationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum/invitations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumInvitationController {

    private final ForumInvitationService forumInvitationService;

    @PostMapping
    public ResponseEntity<?> send(@RequestBody SendInvitationRequest req) {
        try {
            ForumInvitation inv = forumInvitationService.sendInvitation(
                    req.getFromUserId(), req.getToUserId(), req.getMessage());
            return ResponseEntity.status(HttpStatus.CREATED).body(inv);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<ForumInvitation>> inbox(@RequestParam Long userId) {
        return ResponseEntity.ok(forumInvitationService.inbox(userId));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<ForumInvitation>> sent(@RequestParam Long userId) {
        return ResponseEntity.ok(forumInvitationService.sent(userId));
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respond(@PathVariable Long id, @RequestBody RespondRequest req) {
        try {
            return ResponseEntity.ok(forumInvitationService.respond(id, req.getUserId(), req.isAccept()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Data
    public static class SendInvitationRequest {
        private Long fromUserId;
        private Long toUserId;
        private String message;
    }

    @Data
    public static class RespondRequest {
        private Long userId;
        private boolean accept;
    }
}

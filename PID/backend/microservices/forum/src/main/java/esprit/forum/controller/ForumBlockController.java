package esprit.forum.controller;

import esprit.forum.entity.UserBlock;
import esprit.forum.service.ForumBlockService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/forum/blocks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumBlockController {

    private final ForumBlockService forumBlockService;

    @PostMapping
    public ResponseEntity<?> block(@RequestBody BlockRequest req) {
        try {
            UserBlock b = forumBlockService.blockUser(req.getBlockerUserId(), req.getBlockedUserId());
            return ResponseEntity.ok(b);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> unblock(
            @RequestParam Long blockerUserId,
            @RequestParam Long blockedUserId) {
        forumBlockService.unblockUser(blockerUserId, blockedUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{blockerUserId}/ids")
    public ResponseEntity<Set<Long>> blockedIds(@PathVariable Long blockerUserId) {
        return ResponseEntity.ok(forumBlockService.getBlockedUserIds(blockerUserId));
    }

    @Data
    public static class BlockRequest {
        private Long blockerUserId;
        private Long blockedUserId;
    }
}

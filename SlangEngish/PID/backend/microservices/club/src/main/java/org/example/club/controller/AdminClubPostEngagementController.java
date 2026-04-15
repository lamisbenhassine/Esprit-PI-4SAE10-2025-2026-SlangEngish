package org.example.club.controller;

import org.example.club.dto.AdminPostEngagementRowDto;
import org.example.club.service.PostClubEngagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Admin engagement : aussi sous /api/posts-club/admin/engagement pour la gateway /api/posts-club/** */
@RestController
@RequestMapping({
        "/api/admin/club-posts-engagement",
        "/api/posts-club/admin/engagement"
})
@CrossOrigin(origins = "*")
public class AdminClubPostEngagementController {

    @Autowired
    private PostClubEngagementService engagementService;

    /**
     * Vue consolidée pour l’admin : posts, répartition des réactions, fil de commentaires avec noms.
     */
    @GetMapping
    public ResponseEntity<Page<AdminPostEngagementRowDto>> list(
            @RequestParam(required = false) Long clubId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(engagementService.adminEngagementPage(clubId, pageable));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        try {
            engagementService.deleteCommentAsAdmin(commentId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

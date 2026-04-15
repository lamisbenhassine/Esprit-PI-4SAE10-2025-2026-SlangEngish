package org.example.club.controller;

import org.example.club.dto.CommentRequestDto;
import org.example.club.dto.PostCommentViewDto;
import org.example.club.dto.PostEngagementBatchRequestDto;
import org.example.club.dto.PostEngagementSummaryDto;
import org.example.club.dto.ReactionRequestDto;
import org.example.club.entity.PostReactionType;
import org.example.club.service.PostClubEngagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts-club")
@CrossOrigin(origins = "*")
public class PostClubEngagementController {

    @Autowired
    private PostClubEngagementService engagementService;

    @PostMapping("/engagement/batch")
    public ResponseEntity<List<PostEngagementSummaryDto>> batch(@RequestBody PostEngagementBatchRequestDto body) {
        if (body == null || body.getPostIds() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(engagementService.batchSummaries(body.getPostIds(), body.getViewerId()));
    }

    @PostMapping("/{postId}/reactions")
    public ResponseEntity<?> setReaction(@PathVariable Long postId, @RequestBody ReactionRequestDto body) {
        try {
            if (body == null || body.getIdEtudiant() == null || body.getReactionType() == null) {
                return ResponseEntity.badRequest().body("idEtudiant et reactionType requis");
            }
            PostReactionType type = PostReactionType.valueOf(body.getReactionType().trim().toUpperCase());
            engagementService.setReaction(postId, body.getIdEtudiant(), type);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{postId}/reactions")
    public ResponseEntity<?> clearReaction(@PathVariable Long postId, @RequestParam Long userId) {
        try {
            engagementService.clearReaction(postId, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<?> listComments(@PathVariable Long postId) {
        try {
            return ResponseEntity.ok(engagementService.listComments(postId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long postId, @RequestBody CommentRequestDto body) {
        try {
            if (body == null || body.getIdEtudiant() == null) {
                return ResponseEntity.badRequest().body("idEtudiant requis");
            }
            PostCommentViewDto created = engagementService.addComment(
                    postId,
                    body.getIdEtudiant(),
                    body.getContenu(),
                    Boolean.TRUE.equals(body.getTranslateToEnglish()));
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteOwnComment(@PathVariable Long commentId, @RequestParam Long userId) {
        try {
            engagementService.deleteOwnComment(commentId, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}

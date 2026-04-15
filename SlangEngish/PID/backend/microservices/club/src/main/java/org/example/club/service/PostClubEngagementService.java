package org.example.club.service;

import org.example.club.dto.AdminPostEngagementRowDto;
import org.example.club.dto.PostCommentViewDto;
import org.example.club.dto.PostEngagementSummaryDto;
import org.example.club.entity.PostReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostClubEngagementService {

    List<PostEngagementSummaryDto> batchSummaries(List<Long> postIds, Long viewerId);

    void setReaction(Long postId, Long userId, PostReactionType type);

    void clearReaction(Long postId, Long userId);

    List<PostCommentViewDto> listComments(Long postId);

    PostCommentViewDto addComment(Long postId, Long userId, String contenu, boolean translateToEnglish);

    void deleteOwnComment(Long commentId, Long userId);

    void deleteCommentAsAdmin(Long commentId);

    Page<AdminPostEngagementRowDto> adminEngagementPage(Long clubIdFilter, Pageable pageable);
}

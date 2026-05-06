package org.example.club.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AdminPostEngagementRowDto {
    private Long postId;
    private Long clubId;
    private String clubNom;
    private String contenuApercu;
    private String datePost;
    private Map<String, Long> reactionCounts = new LinkedHashMap<>();
    private long totalReactions;
    private long totalComments;
    private List<PostCommentViewDto> comments = new ArrayList<>();
}

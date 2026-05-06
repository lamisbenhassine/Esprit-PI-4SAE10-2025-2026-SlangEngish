package org.example.club.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class PostEngagementSummaryDto {
    private Long postId;
    /** Ex. LIKE -> 3 */
    private Map<String, Long> reactionCounts = new LinkedHashMap<>();
    /** Type choisi par le viewer, ou null */
    private String myReaction;
    private long commentCount;
}

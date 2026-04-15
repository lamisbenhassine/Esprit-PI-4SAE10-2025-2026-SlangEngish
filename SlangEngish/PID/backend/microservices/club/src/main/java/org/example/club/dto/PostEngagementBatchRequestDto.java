package org.example.club.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PostEngagementBatchRequestDto {
    private List<Long> postIds = new ArrayList<>();
    /** Optionnel : réaction courante de cet utilisateur */
    private Long viewerId;
}

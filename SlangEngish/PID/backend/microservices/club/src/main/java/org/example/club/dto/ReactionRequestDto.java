package org.example.club.dto;

import lombok.Data;

@Data
public class ReactionRequestDto {
    private Long idEtudiant;
    /** Valeur de {@link org.example.club.entity.PostReactionType} */
    private String reactionType;
}

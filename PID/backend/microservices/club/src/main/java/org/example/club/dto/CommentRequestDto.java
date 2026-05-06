package org.example.club.dto;

import lombok.Data;

@Data
public class CommentRequestDto {
    private Long idEtudiant;
    private String contenu;
    private Boolean translateToEnglish;
}

package org.example.club.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCommentViewDto {
    private Long id;
    private Long idAuteur;
    private String auteurNom;
    private String contenu;
    private String dateCreation;
    private String aiSentiment;
    private boolean aiCorrectionApplied;
    private boolean aiTranslatedToEnglish;
}

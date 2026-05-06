package org.example.club.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackClubDto {
    private Long id;
    private Long idEtudiant;
    private Long idClub;
    private String nomClub;
    private Integer note;
    private String commentaire;
    private LocalDateTime dateCreation;
    private String prenomEtudiant;
    private String nomEtudiant;
    /** POSITIVE | NEGATIVE | NEUTRAL */
    private String sentiment;
}


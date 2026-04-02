package org.example.evenement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDto {
    private Long id;
    private Long idEtudiant;
    private Long evenementId;
    private Integer note;
    private String commentaire;
    private LocalDateTime dateCreation;
    private String nomEtudiant;  // Pour l'affichage admin
    /** POSITIVE | NEGATIVE | NEUTRAL — analyse lexicale + note */
    private String sentiment;
}

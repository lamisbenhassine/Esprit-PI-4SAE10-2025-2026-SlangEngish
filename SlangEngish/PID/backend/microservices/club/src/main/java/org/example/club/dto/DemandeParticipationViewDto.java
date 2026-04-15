package org.example.club.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.club.entity.StatutParticipation;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeParticipationViewDto {
    private Long id;
    private Long idEtudiant;
    private Long idClub;
    private String nomClub;
    private LocalDateTime dateDemande;
    private StatutParticipation statut;
    /**
     * Score basé sur valeurs numériques (0-100)
     */
    private Double score;
    private String reponsesFormulaire;
    private String texteMotivation;
    private String departementSouhaite;
    private String departementAssigne;
    private String nomEtudiant;
    private String prenomEtudiant;
    private String emailEtudiant;
}

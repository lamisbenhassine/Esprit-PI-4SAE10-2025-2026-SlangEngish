package org.example.club.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeParticipationDto {
    private Long idEtudiant;
    private Long idClub;
    /**
     * Texte de motivation - utilisé pour calculer le score automatiquement
     */
    private String texteMotivation;
    /**
     * Réponses numériques (optionnel, pour rétrocompatibilité)
     */
    private Map<String, Integer> reponses;

    /**
     * Département choisi par l'étudiant
     */
    private String departementSouhaite;
}

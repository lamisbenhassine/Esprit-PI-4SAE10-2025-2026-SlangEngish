package org.example.evenement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesEvenementDto {

    private Double tauxRemplissage;           // % moyen de remplissage (événements avec capacité)
    private Double moyenneParticipation;       // Moyenne inscriptions par événement
    private EvenementPopulaireDto plusPopulaire;  // Événement le plus populaire
    private List<EvolutionMensuelleDto> evolutionMensuelle;  // Inscriptions par mois
}

package esprit.inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Métier avancé 1 (style Duolingo) : scoring "qui devrait s'abonner" selon temps d'étude, niveau, objectif.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDTO {
    /** Score 0-100 : pertinence de l'abonnement pour ce profil. */
    private int score;
    /** Message explicatif (ex. "Recommandé si vous étudiez 20+ min/jour"). */
    private String message;
    /** Niveau suggéré (ex. B1). */
    private String suggestedLevel;
    /** Id du plan recommandé si un plan correspond au niveau. */
    private Long recommendedPlanId;
    private String recommendedPlanName;
}

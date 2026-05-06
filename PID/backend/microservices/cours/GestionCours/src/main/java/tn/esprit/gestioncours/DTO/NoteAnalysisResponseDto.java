package tn.esprit.gestioncours.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse de l'analyse IA : comparaison avec la version enregistrée + propositions (pas d'écriture en base).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteAnalysisResponseDto {

    /** Résumé encourageant pour l'apprenant. */
    private String feedbackSummary;

    /** Titre proposé (vide ou null si le titre actuel est déjà satisfaisant). */
    private String suggestedTitle;

    /** Corps avec corrections orthographiques et grammaticales (HTML fragment). */
    private String grammarCorrectedHtml;

    /** Version finale réorganisée et clarifiée (HTML fragment). */
    private String restructuredHtml;

    /** Copie de la note au moment de l'analyse (enregistrée en base). */
    private String originalTitle;

    private String originalHtml;
}

package tn.esprit.gestioncours.DTO.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Structure JSON attendue du modèle (response_format json_object).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoteAiJsonPayload {

    private String feedbackSummary;
    private String suggestedTitle;
    private String grammarCorrectedHtml;
    private String restructuredHtml;
}

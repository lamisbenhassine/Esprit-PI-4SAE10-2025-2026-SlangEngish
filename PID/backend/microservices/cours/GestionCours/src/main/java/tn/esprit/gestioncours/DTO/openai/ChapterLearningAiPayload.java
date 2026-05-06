package tn.esprit.gestioncours.DTO.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChapterLearningAiPayload {

    private String summary;
    private List<FlashcardAi> flashcards;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FlashcardAi {
        private String front;
        private String back;
    }
}

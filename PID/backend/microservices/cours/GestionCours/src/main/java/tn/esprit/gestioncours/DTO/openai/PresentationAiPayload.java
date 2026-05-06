package tn.esprit.gestioncours.DTO.openai;

import lombok.Data;

import java.util.List;

@Data
public class PresentationAiPayload {
    private List<SlideAi> slides;
    private String narration;

    @Data
    public static class SlideAi {
        private String title;
        private List<String> bullets;
    }
}

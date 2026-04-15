package esprit.forum.dto;

import lombok.Data;

@Data
public class AiTranslateRequest {
    private String text;
    /** Code ISO 639-1 (ex. fr, en, ar, es). */
    private String targetLanguage;
}

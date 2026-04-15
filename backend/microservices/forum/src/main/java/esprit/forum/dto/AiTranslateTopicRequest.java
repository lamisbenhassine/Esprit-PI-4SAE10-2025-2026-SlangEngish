package esprit.forum.dto;

import lombok.Data;

@Data
public class AiTranslateTopicRequest {
    private String title;
    private String description;
    /** Code ISO 639-1 (ex. fr, en, ar). */
    private String targetLanguage;
}

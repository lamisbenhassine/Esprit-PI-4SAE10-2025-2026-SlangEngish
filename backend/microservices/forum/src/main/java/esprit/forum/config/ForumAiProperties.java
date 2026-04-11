package esprit.forum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "forum.ai")
public class ForumAiProperties {

    /**
     * Google Gemini API key (recommended via env var FORUM_AI_API_KEY).
     */
    private String apiKey = "";

    /**
     * Gemini model name (without URL path prefix).
     */
    private String model = "gemini-2.0-flash";

    /**
     * Max characters accepted by the summarization endpoint (basic abuse protection).
     */
    private int maxInputChars = 12000;
}

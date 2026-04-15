package esprit.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTranslateTopicResponse {
    private String trTitle;
    private String trDescription;
}

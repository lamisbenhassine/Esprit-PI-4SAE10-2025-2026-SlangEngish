package esprit.forum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModerationMessageDto {
    private Long id;
    private Long topicId;
    private String topicTitle;
    private Long authorId;
    private String content;
    private String attachments;
    private Long parentMessageId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

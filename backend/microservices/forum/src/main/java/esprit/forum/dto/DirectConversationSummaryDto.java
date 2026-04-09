package esprit.forum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import esprit.forum.entity.DirectConversation;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DirectConversationSummaryDto {
    private Long id;
    private Long otherUserId;
    private DirectConversation.DirectKind kind;
    private String lastMessagePreview;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}

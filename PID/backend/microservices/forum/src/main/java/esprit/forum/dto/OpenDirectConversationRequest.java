package esprit.forum.dto;

import esprit.forum.entity.DirectConversation;
import lombok.Data;

@Data
public class OpenDirectConversationRequest {
    private Long userId;
    private Long withUserId;
    private DirectConversation.DirectKind kind;
}

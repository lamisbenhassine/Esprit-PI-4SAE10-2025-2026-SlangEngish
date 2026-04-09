package esprit.forum.dto;

import lombok.Data;

@Data
public class SendDirectMessageRequest {
    private Long senderId;
    private String content;
}

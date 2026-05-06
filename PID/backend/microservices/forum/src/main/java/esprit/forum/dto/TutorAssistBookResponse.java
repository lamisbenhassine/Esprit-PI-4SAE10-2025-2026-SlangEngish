package esprit.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TutorAssistBookResponse {
    private boolean success;
    private String appointmentId;
    private String meetLink;
}

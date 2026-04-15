package esprit.forum.dto;

import lombok.Data;

@Data
public class TutorAssistAvailabilityRequest {
    private String start;
    private String end;
    private Long tutorId;
}

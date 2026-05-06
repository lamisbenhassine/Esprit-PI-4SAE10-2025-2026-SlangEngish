package esprit.forum.dto;

import lombok.Data;

@Data
public class TutorAssistAppointmentCallbackRequest {
    private Long topicId;
    private Long studentId;
    private Long tutorId;
    private String tutorName;
    private String start;
    private String end;
    private String timezone;
    private String appointmentId;
    private String meetLink;
    private String problemType;
}

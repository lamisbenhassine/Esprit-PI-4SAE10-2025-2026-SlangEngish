package esprit.forum.dto;

import lombok.Data;

@Data
public class TutorAssistBookRequest {
    private Long topicId;
    private Long studentId;
    private String studentName;
    private Long tutorId;
    private String tutorName;
    private String start;
    private String end;
    private String timezone;
    private String problemType;
    private String meetingMode;
}

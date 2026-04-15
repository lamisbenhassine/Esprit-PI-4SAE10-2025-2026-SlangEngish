package esprit.forum.dto;

import lombok.Data;

@Data
public class TutorAssistSlotsRequest {
    private Long topicId;
    private Long studentId;
    private String studentName;
    private String level;
    private String problemType;
    private String timezone;
    private Integer durationMin;
    private String windowStart;
    private String windowEnd;
}

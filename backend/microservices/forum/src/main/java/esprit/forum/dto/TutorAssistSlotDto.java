package esprit.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TutorAssistSlotDto {
    private String start;
    private String end;
    private Long tutorId;
    private String tutorName;
}

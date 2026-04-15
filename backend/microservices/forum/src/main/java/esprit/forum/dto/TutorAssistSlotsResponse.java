package esprit.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TutorAssistSlotsResponse {
    private List<TutorAssistSlotDto> slots = new ArrayList<>();
}

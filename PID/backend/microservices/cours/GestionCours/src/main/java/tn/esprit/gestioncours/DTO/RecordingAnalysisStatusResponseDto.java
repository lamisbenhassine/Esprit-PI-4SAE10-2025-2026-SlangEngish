package tn.esprit.gestioncours.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.gestioncours.Entities.RecordingAnalysisStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordingAnalysisStatusResponseDto {
    private RecordingAnalysisStatus status;
}

package tn.esprit.gestioncours.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteAnalysisErrorDto {
    private String error;
    private String message;
}

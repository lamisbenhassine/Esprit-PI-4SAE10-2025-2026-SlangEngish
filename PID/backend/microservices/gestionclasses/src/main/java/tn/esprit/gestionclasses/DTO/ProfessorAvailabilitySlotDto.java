package tn.esprit.gestionclasses.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorAvailabilitySlotDto {
    private String slotCode;
    /** Texte libre (ex. contenu prévu pour la séance). Optionnel. */
    private String description;
}

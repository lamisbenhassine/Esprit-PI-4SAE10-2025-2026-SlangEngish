package tn.esprit.gestionclasses.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorAvailabilitySaveRequestDto {
    private List<ProfessorAvailabilitySlotDto> slots;
}

package tn.esprit.gestionclasses.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.gestionclasses.DTO.ProfessorAvailabilitySaveRequestDto;
import tn.esprit.gestionclasses.DTO.ProfessorAvailabilitySlotDto;
import tn.esprit.gestionclasses.Entities.ProfessorSessionAvailability;
import tn.esprit.gestionclasses.Repositories.ProfessorSessionAvailabilityRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessorAvailabilityServiceTest {

    @Mock
    private ProfessorSessionAvailabilityRepository repository;

    @InjectMocks
    private ProfessorAvailabilityService service;

    @Test
    void save_shouldRejectDuplicateSlots() {
        ProfessorAvailabilitySaveRequestDto request = new ProfessorAvailabilitySaveRequestDto(List.of(
                ProfessorAvailabilitySlotDto.builder().slotCode("SAT_09_12").description("A").build(),
                ProfessorAvailabilitySlotDto.builder().slotCode("SAT_09_12").description("B").build()
        ));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.save(7L, request)
        );

        assertEquals("Créneau dupliqué: SAT_09_12", ex.getMessage());
    }

    @Test
    void save_shouldPersistCleanedSlotsAndReturnSortedAvailability() {
        Long professorId = 3L;
        ProfessorAvailabilitySaveRequestDto request = new ProfessorAvailabilitySaveRequestDto(List.of(
                ProfessorAvailabilitySlotDto.builder().slotCode(" SUN_17_20 ").description("  Session finale ").build(),
                ProfessorAvailabilitySlotDto.builder().slotCode("TUE_17_20").description(" ").build()
        ));

        when(repository.findByProfessorUserId(professorId)).thenReturn(List.of(
                row(professorId, "TUE_17_20", null),
                row(professorId, "SUN_17_20", "Session finale")
        ));

        var response = service.save(professorId, request);

        verify(repository).deleteByProfessorUserId(professorId);
        verify(repository).flush();
        verify(repository).saveAll(anyList());
        assertEquals(2, response.getSlots().size());
        assertEquals("SUN_17_20", response.getSlots().get(0).getSlotCode());
        assertEquals("Session finale", response.getSlots().get(0).getDescription());
        assertEquals("", response.getSlots().get(1).getDescription());
    }

    private ProfessorSessionAvailability row(Long professorId, String slot, String description) {
        ProfessorSessionAvailability row = new ProfessorSessionAvailability();
        row.setProfessorUserId(professorId);
        row.setSlotCode(slot);
        row.setDescription(description);
        return row;
    }
}

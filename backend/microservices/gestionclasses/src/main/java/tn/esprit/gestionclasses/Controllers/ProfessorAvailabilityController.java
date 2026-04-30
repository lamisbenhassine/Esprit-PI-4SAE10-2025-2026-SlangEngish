package tn.esprit.gestionclasses.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.gestionclasses.DTO.ProfessorAvailabilityResponseDto;
import tn.esprit.gestionclasses.DTO.ProfessorAvailabilitySaveRequestDto;
import tn.esprit.gestionclasses.Services.ProfessorAvailabilityService;

import java.util.Map;

@RestController
@RequestMapping("/professor-availability")
@RequiredArgsConstructor
public class ProfessorAvailabilityController {

    private final ProfessorAvailabilityService availabilityService;

    @GetMapping
    public ResponseEntity<ProfessorAvailabilityResponseDto> get(@RequestParam Long professorUserId) {
        return ResponseEntity.ok(availabilityService.getAvailability(professorUserId));
    }

    @PutMapping
    public ResponseEntity<?> put(
            @RequestParam Long professorUserId,
            @RequestBody ProfessorAvailabilitySaveRequestDto body) {
        try {
            return ResponseEntity.ok(availabilityService.save(professorUserId, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}

package org.example.club.controller;

import org.example.club.entity.ReunionClub;
import org.example.club.service.ReunionClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reunions-club")
@CrossOrigin(origins = "*")
public class ReunionClubController {

    @Autowired
    private ReunionClubService reunionService;

    // CREATE - Créer une nouvelle réunion
    @PostMapping
    public ResponseEntity<?> createReunion(@RequestBody ReunionClub reunion) {
        try {
            ReunionClub createdReunion = reunionService.createReunion(reunion);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdReunion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création de la réunion: " + e.getMessage());
        }
    }

    // READ - Récupérer toutes les réunions
    @GetMapping
    public ResponseEntity<List<ReunionClub>> getAllReunions() {
        List<ReunionClub> reunions = reunionService.getAllReunions();
        return ResponseEntity.ok(reunions);
    }

    // READ - Récupérer une réunion par son ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getReunionById(@PathVariable Long id) {
        Optional<ReunionClub> reunion = reunionService.getReunionById(id);

        if (reunion.isPresent()) {
            return ResponseEntity.ok(reunion.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Réunion non trouvée avec l'ID: " + id);
        }
    }

    // UPDATE - Mettre à jour une réunion
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReunion(@PathVariable Long id, @RequestBody ReunionClub reunion) {
        try {
            ReunionClub updatedReunion = reunionService.updateReunion(id, reunion);
            return ResponseEntity.ok(updatedReunion);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise à jour de la réunion: " + e.getMessage());
        }
    }

    // DELETE - Supprimer une réunion
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReunion(@PathVariable Long id) {
        try {
            reunionService.deleteReunion(id);
            return ResponseEntity.ok("Réunion supprimée avec succès");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression de la réunion: " + e.getMessage());
        }
    }

    // Récupérer les réunions d'un club
    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<ReunionClub>> getReunionsByClub(@PathVariable Long clubId) {
        List<ReunionClub> reunions = reunionService.getReunionsByClub(clubId);
        return ResponseEntity.ok(reunions);
    }

    // Récupérer les réunions par date
    @GetMapping("/date/{date}")
    public ResponseEntity<List<ReunionClub>> getReunionsByDate(@PathVariable LocalDate date) {
        List<ReunionClub> reunions = reunionService.getReunionsByDate(date);
        return ResponseEntity.ok(reunions);
    }

    // Récupérer les réunions d'un club par date
    @GetMapping("/club/{clubId}/date/{date}")
    public ResponseEntity<List<ReunionClub>> getReunionsByClubAndDate(
            @PathVariable Long clubId, 
            @PathVariable LocalDate date) {
        List<ReunionClub> reunions = reunionService.getReunionsByClubAndDate(clubId, date);
        return ResponseEntity.ok(reunions);
    }

    // Récupérer les réunions d'un club, triées par date décroissante
    @GetMapping("/club/{clubId}/recent")
    public ResponseEntity<List<ReunionClub>> getReunionsByClubOrderByDateDesc(@PathVariable Long clubId) {
        List<ReunionClub> reunions = reunionService.getReunionsByClubOrderByDateDesc(clubId);
        return ResponseEntity.ok(reunions);
    }

    // Récupérer les réunions à venir
    @GetMapping("/avenir/{date}")
    public ResponseEntity<List<ReunionClub>> getReunionsAVenir(@PathVariable LocalDate date) {
        List<ReunionClub> reunions = reunionService.getReunionsAVenir(date);
        return ResponseEntity.ok(reunions);
    }
}


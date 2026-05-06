package org.example.club.controller;

import org.example.club.entity.Club;
import org.example.club.service.ClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clubs")
@CrossOrigin(origins = "*")
public class ClubController {

    @Autowired
    private ClubService clubService;

    // CREATE - Créer un nouveau club
    @PostMapping
    public ResponseEntity<?> createClub(@RequestBody Club club) {
        try {
            Club createdClub = clubService.createClub(club);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdClub);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création du club: " + e.getMessage());
        }
    }

    // READ - Récupérer tous les clubs
    @GetMapping
    public ResponseEntity<List<Club>> getAllClubs() {
        List<Club> clubs = clubService.getAllClubs();
        return ResponseEntity.ok(clubs);
    }

    // READ - Récupérer un club par son ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getClubById(@PathVariable Long id) {
        Optional<Club> club = clubService.getClubById(id);

        if (club.isPresent()) {
            return ResponseEntity.ok(club.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Club non trouvé avec l'ID: " + id);
        }
    }

    // UPDATE - Mettre à jour un club
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClub(@PathVariable Long id, @RequestBody Club club) {
        try {
            Club updatedClub = clubService.updateClub(id, club);
            return ResponseEntity.ok(updatedClub);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise à jour du club: " + e.getMessage());
        }
    }

    // DELETE - Supprimer un club
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClub(@PathVariable Long id) {
        try {
            clubService.deleteClub(id);
            return ResponseEntity.ok("Club supprimé avec succès");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression du club: " + e.getMessage());
        }
    }

    // Recherche par nom
    @GetMapping("/nom/{nom}")
    public ResponseEntity<?> getClubByNom(@PathVariable String nom) {

        Optional<Club> club = clubService.getClubByNom(nom);

        if (club.isPresent()) {
            return ResponseEntity.ok(club.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Club non trouvé avec le nom: " + nom);
        }
    }


    // Recherche par type
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Club>> getClubsByType(@PathVariable String type) {
        List<Club> clubs = clubService.getClubsByType(type);
        return ResponseEntity.ok(clubs);
    }

    // Recherche par statut
    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<Club>> getClubsByStatut(@PathVariable String statut) {
        List<Club> clubs = clubService.getClubsByStatut(statut);
        return ResponseEntity.ok(clubs);
    }

    // Recherche par responsable
    @GetMapping("/responsable/{idResponsable}")
    public ResponseEntity<List<Club>> getClubsByResponsable(@PathVariable Long idResponsable) {
        List<Club> clubs = clubService.getClubsByResponsable(idResponsable);
        return ResponseEntity.ok(clubs);
    }

    @GetMapping("/{id}/departements")
    public ResponseEntity<List<String>> getDepartementsClub(@PathVariable Long id) {
        Optional<Club> club = clubService.getClubById(id);
        if (club.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
        String csv = club.get().getDepartements();
        if (csv == null || csv.isBlank()) {
            return ResponseEntity.ok(List.of("RH", "Marketing", "Technique", "Finance"));
        }
        List<String> deps = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        return ResponseEntity.ok(deps);
    }
}


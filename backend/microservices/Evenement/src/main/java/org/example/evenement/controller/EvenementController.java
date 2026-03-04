package org.example.evenement.controller;

import org.example.evenement.entity.Evenement;
import org.example.evenement.entity.EventStatus;
import org.example.evenement.service.EvenementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/evenements")
@CrossOrigin(origins = "*")
public class EvenementController {

    @Autowired
    private EvenementService evenementService;

    // CREATE - Créer un nouvel événement
    @PostMapping
    public ResponseEntity<?> createEvenement(@RequestBody Evenement evenement) {
        try {
            Evenement createdEvenement = evenementService.createEvenement(evenement);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdEvenement);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création de l'événement: " + e.getMessage());
        }
    }

    // READ - Récupérer tous les événements
    @GetMapping
    public ResponseEntity<List<Evenement>> getAllEvenements() {
        List<Evenement> evenements = evenementService.getAllEvenements();
        return ResponseEntity.ok(evenements);
    }

    // READ - Récupérer un événement par son ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getEvenementById(@PathVariable Long id) {
        Optional<Evenement> evenement = evenementService.getEvenementById(id);

        if (evenement.isPresent()) {
            return ResponseEntity.ok(evenement.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Événement non trouvé avec l'ID: " + id);
        }
    }

    // UPDATE - Mettre à jour un événement
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvenement(@PathVariable Long id, @RequestBody Evenement evenement) {
        try {
            Evenement updatedEvenement = evenementService.updateEvenement(id, evenement);
            return ResponseEntity.ok(updatedEvenement);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise à jour de l'événement: " + e.getMessage());
        }
    }

    // DELETE - Supprimer un événement
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvenement(@PathVariable Long id) {
        try {
            evenementService.deleteEvenement(id);
            return ResponseEntity.ok("Événement supprimé avec succès");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression de l'événement: " + e.getMessage());
        }
    }

    // Recherche par titre
    @GetMapping("/titre/{titre}")
    public ResponseEntity<?> getEvenementByTitre(@PathVariable String titre) {
        Optional<Evenement> evenement = evenementService.getEvenementByTitre(titre);

        if (evenement.isPresent()) {
            return ResponseEntity.ok(evenement.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Événement non trouvé avec le titre: " + titre);
        }
    }

    // Recherche par type
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Evenement>> getEvenementsByType(@PathVariable String type) {
        List<Evenement> evenements = evenementService.getEvenementsByType(type);
        return ResponseEntity.ok(evenements);
    }

    // Recherche par statut
    @GetMapping("/statut/{status}")
    public ResponseEntity<List<Evenement>> getEvenementsByStatus(@PathVariable EventStatus status) {
        List<Evenement> evenements = evenementService.getEvenementsByStatus(status);
        return ResponseEntity.ok(evenements);
    }

    // Recherche par date
    @GetMapping("/date/{date}")
    public ResponseEntity<List<Evenement>> getEvenementsByDate(@PathVariable LocalDate date) {
        List<Evenement> evenements = evenementService.getEvenementsByDate(date);
        return ResponseEntity.ok(evenements);
    }

    // Recherche par lieu
    @GetMapping("/lieu/{lieu}")
    public ResponseEntity<List<Evenement>> getEvenementsByLieu(@PathVariable String lieu) {
        List<Evenement> evenements = evenementService.getEvenementsByLieu(lieu);
        return ResponseEntity.ok(evenements);
    }

    // Recherche des événements à venir
    @GetMapping("/avenir/{date}")
    public ResponseEntity<List<Evenement>> getEvenementsAVenir(@PathVariable LocalDate date) {
        List<Evenement> evenements = evenementService.getEvenementsAVenir(date);
        return ResponseEntity.ok(evenements);
    }
}


package org.example.evenement.controller;

import org.example.evenement.dto.InscriptionResultDto;
import org.example.evenement.dto.StatutInscriptionDto;
import org.example.evenement.entity.InscriptionEvenement;
import org.example.evenement.service.InscriptionEvenementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/inscriptions")
@CrossOrigin(origins = "*")
public class InscriptionEvenementController {

    @Autowired
    private InscriptionEvenementService inscriptionService;

    // CREATE - Créer une nouvelle inscription (ou liste d'attente si complet)
    @PostMapping
    public ResponseEntity<?> createInscription(@RequestBody InscriptionEvenement inscription) {
        try {
            InscriptionResultDto result = inscriptionService.createInscription(inscription);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création de l'inscription: " + e.getMessage());
        }
    }

    // READ - Récupérer toutes les inscriptions
    @GetMapping
    public ResponseEntity<List<InscriptionEvenement>> getAllInscriptions() {
        List<InscriptionEvenement> inscriptions = inscriptionService.getAllInscriptions();
        return ResponseEntity.ok(inscriptions);
    }

    // READ - Récupérer une inscription par son ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getInscriptionById(@PathVariable Long id) {
        Optional<InscriptionEvenement> inscription = inscriptionService.getInscriptionById(id);

        if (inscription.isPresent()) {
            return ResponseEntity.ok(inscription.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Inscription non trouvée avec l'ID: " + id);
        }
    }

    // UPDATE - Mettre à jour une inscription
    @PutMapping("/{id}")
    public ResponseEntity<?> updateInscription(@PathVariable Long id, @RequestBody InscriptionEvenement inscription) {
        try {
            InscriptionEvenement updatedInscription = inscriptionService.updateInscription(id, inscription);
            return ResponseEntity.ok(updatedInscription);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise à jour de l'inscription: " + e.getMessage());
        }
    }

    // DELETE - Supprimer une inscription
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInscription(@PathVariable Long id) {
        try {
            inscriptionService.deleteInscription(id);
            return ResponseEntity.ok("Inscription supprimée avec succès");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression de l'inscription: " + e.getMessage());
        }
    }

    // Récupérer les inscriptions d'un étudiant
    @GetMapping("/etudiant/{idEtudiant}")
    public ResponseEntity<List<InscriptionEvenement>> getInscriptionsByEtudiant(@PathVariable Long idEtudiant) {
        List<InscriptionEvenement> inscriptions = inscriptionService.getInscriptionsByEtudiant(idEtudiant);
        return ResponseEntity.ok(inscriptions);
    }

    // Récupérer les inscriptions d'un événement
    @GetMapping("/evenement/{evenementId}")
    public ResponseEntity<List<InscriptionEvenement>> getInscriptionsByEvenement(@PathVariable Long evenementId) {
        List<InscriptionEvenement> inscriptions = inscriptionService.getInscriptionsByEvenement(evenementId);
        return ResponseEntity.ok(inscriptions);
    }

    // Vérifier si un étudiant est inscrit à un événement
    @GetMapping("/verifier/{idEtudiant}/{evenementId}")
    public ResponseEntity<?> isEtudiantInscrit(@PathVariable Long idEtudiant, @PathVariable Long evenementId) {
        boolean isInscrit = inscriptionService.isEtudiantInscrit(idEtudiant, evenementId);
        return ResponseEntity.ok(isInscrit);
    }

    // Obtenir le statut d'inscription (inscrit, liste d'attente, position)
    @GetMapping("/statut/{idEtudiant}/{evenementId}")
    public ResponseEntity<StatutInscriptionDto> getStatutInscription(@PathVariable Long idEtudiant, @PathVariable Long evenementId) {
        StatutInscriptionDto statut = inscriptionService.getStatutInscription(idEtudiant, evenementId);
        return ResponseEntity.ok(statut);
    }

    // Compter les inscriptions pour un événement
    @GetMapping("/count/evenement/{evenementId}")
    public ResponseEntity<Long> countInscriptionsByEvenement(@PathVariable Long evenementId) {
        long count = inscriptionService.countInscriptionsByEvenement(evenementId);
        return ResponseEntity.ok(count);
    }

    // Désinscrire un étudiant d'un événement (retourne JSON pour cohérence avec le frontend)
    @DeleteMapping("/desinscrire/{idEtudiant}/{evenementId}")
    public ResponseEntity<?> desinscrireEtudiant(@PathVariable Long idEtudiant, @PathVariable Long evenementId) {
        try {
            inscriptionService.desinscrireEtudiant(idEtudiant, evenementId);
            return ResponseEntity.ok(java.util.Map.of("message", "Étudiant désinscrit avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Erreur lors de la désinscription: " + e.getMessage()));
        }
    }
}


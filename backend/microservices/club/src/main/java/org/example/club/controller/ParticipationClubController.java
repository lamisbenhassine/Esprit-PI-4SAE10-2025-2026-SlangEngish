package org.example.club.controller;

import org.example.club.dto.DemandeParticipationDto;
import org.example.club.dto.DemandeParticipationViewDto;
import org.example.club.dto.ResultDemandeDto;
import org.example.club.service.ParticipationClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs/participation")
@CrossOrigin(origins = "*")
public class ParticipationClubController {

    @Autowired
    private ParticipationClubService participationService;

    /**
     * Demande de rejoindre un club (étudiant)
     */
    @PostMapping("/demander")
    public ResponseEntity<?> demanderRejoindre(@RequestBody DemandeParticipationDto dto) {
        try {
            if (dto == null || dto.getIdEtudiant() == null || dto.getIdClub() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Données incomplètes (idEtudiant et idClub requis)");
            }
            ResultDemandeDto result = participationService.demanderRejoindre(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur serveur: " + e.getMessage());
        }
    }

    /**
     * Liste des demandes en attente (backoffice)
     * @param clubId optionnel - filtrer par club
     */
    @GetMapping("/demandes-en-attente")
    public ResponseEntity<List<DemandeParticipationViewDto>> getDemandesEnAttente(
            @RequestParam(required = false) Long clubId) {
        List<DemandeParticipationViewDto> list = participationService.getDemandesEnAttente(clubId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/accepter")
    public ResponseEntity<?> accepter(@PathVariable Long id,
                                      @RequestParam(required = false) String departement) {
        try {
            participationService.accepter(id, departement);
            return ResponseEntity.ok("Demande acceptée");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/refuser")
    public ResponseEntity<?> refuser(@PathVariable Long id) {
        try {
            participationService.refuser(id);
            return ResponseEntity.ok("Demande refusée");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/statut/{idEtudiant}/{idClub}")
    public ResponseEntity<java.util.Map<String, String>> getStatutParticipation(@PathVariable Long idEtudiant, @PathVariable Long idClub) {
        var statut = participationService.getStatutParticipation(idEtudiant, idClub);
        return ResponseEntity.ok(java.util.Collections.singletonMap("statut", statut != null ? statut.name() : null));
    }

    @GetMapping("/mes-clubs/{idEtudiant}")
    public ResponseEntity<?> getClubsMembre(@PathVariable Long idEtudiant) {
        return ResponseEntity.ok(participationService.getClubsMembre(idEtudiant));
    }

    @GetMapping("/affectation/{idEtudiant}/{idClub}")
    public ResponseEntity<java.util.Map<String, String>> getDepartementAffecte(@PathVariable Long idEtudiant,
                                                                                @PathVariable Long idClub) {
        String dep = participationService.getDepartementAffecte(idEtudiant, idClub);
        return ResponseEntity.ok(java.util.Collections.singletonMap("departement", dep));
    }

    @GetMapping("/membres/{idClub}")
    public ResponseEntity<?> getMembresClub(@PathVariable Long idClub) {
        return ResponseEntity.ok(participationService.getMembresClub(idClub));
    }

    @PostMapping("/membres/{participationId}/bloquer")
    public ResponseEntity<?> bloquerMembre(@PathVariable Long participationId) {
        try {
            participationService.bloquerMembre(participationId);
            return ResponseEntity.ok("Membre bloqué");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/membres/{participationId}")
    public ResponseEntity<?> supprimerMembre(@PathVariable Long participationId) {
        try {
            participationService.supprimerMembre(participationId);
            return ResponseEntity.ok("Membre supprimé");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}

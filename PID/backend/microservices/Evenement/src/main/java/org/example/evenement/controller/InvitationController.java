package org.example.evenement.controller;

import org.example.evenement.client.UserApiClient;
import org.example.evenement.client.UserInfoDto;
import org.example.evenement.dto.InvitationDto;
import org.example.evenement.entity.Evenement;
import org.example.evenement.entity.InscriptionEvenement;
import org.example.evenement.service.InscriptionEvenementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/invitation")
@CrossOrigin(origins = "*")
public class InvitationController {

    @Autowired
    private InscriptionEvenementService inscriptionService;

    @Autowired
    private UserApiClient userApiClient;

    @GetMapping("/{inscriptionId}")
    public ResponseEntity<?> getInvitation(@PathVariable Long inscriptionId) {
        Optional<InscriptionEvenement> opt = inscriptionService.getInscriptionById(inscriptionId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InscriptionEvenement ins = opt.get();
        Evenement ev = ins.getEvenement();
        if (ev == null) {
            return ResponseEntity.notFound().build();
        }
        UserInfoDto user = userApiClient.getUserById(ins.getIdEtudiant());
        String nom = user != null && user.getLastName() != null ? user.getLastName() : "Invité";
        String prenom = user != null ? user.getFirstName() : "";
        String email = user != null ? user.getEmail() : "";
        InvitationDto dto = new InvitationDto(
                ins.getId(),
                ev.getId(),
                ev.getTitre(),
                ev.getDescription(),
                ev.getDate(),
                ev.getHeure(),
                ev.getLieu(),
                ev.getType(),
                nom,
                prenom,
                email
        );
        return ResponseEntity.ok(dto);
    }
}

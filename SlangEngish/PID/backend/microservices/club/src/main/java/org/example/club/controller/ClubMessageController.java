package org.example.club.controller;

import org.example.club.dto.ClubMessageDto;
import org.example.club.service.ClubMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clubs/messages")
@CrossOrigin(origins = "*")
public class ClubMessageController {

    @Autowired
    private ClubMessageService messageService;

    @GetMapping("/{clubId}")
    public ResponseEntity<?> getMessages(@PathVariable Long clubId,
                                         @RequestParam Long idEtudiant,
                                         @RequestParam(required = false) String scope,
                                         @RequestParam(required = false) String departement) {
        try {
            List<ClubMessageDto> messages = messageService.getMessagesByClub(clubId, idEtudiant, scope, departement);
            return ResponseEntity.ok(messages);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping("/{clubId}")
    public ResponseEntity<?> envoyerMessage(@PathVariable Long clubId, @RequestBody Map<String, Object> body) {
        try {
            Long idEtudiant = Long.valueOf(body.get("idEtudiant").toString());
            String contenu = (String) body.get("contenu");
            String scope = body.get("scope") != null ? body.get("scope").toString() : null;
            String departement = body.get("departement") != null ? body.get("departement").toString() : null;
            ClubMessageDto msg = messageService.envoyerMessage(clubId, idEtudiant, contenu, scope, departement);
            return ResponseEntity.status(HttpStatus.CREATED).body(msg);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}

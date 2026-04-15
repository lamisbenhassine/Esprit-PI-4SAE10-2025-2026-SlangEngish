package org.example.evenement.controller;

import org.example.evenement.dto.StatistiquesEvenementDto;
import org.example.evenement.service.StatistiquesEvenementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evenements/statistiques")
@CrossOrigin(origins = "*")
public class StatistiquesController {

    @Autowired
    private StatistiquesEvenementService statistiquesService;

    @GetMapping
    public ResponseEntity<StatistiquesEvenementDto> getStatistiques() {
        return ResponseEntity.ok(statistiquesService.getStatistiques());
    }
}

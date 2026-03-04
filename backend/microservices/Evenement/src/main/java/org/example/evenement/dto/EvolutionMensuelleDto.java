package org.example.evenement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvolutionMensuelleDto {
    private String mois;   // Format "2024-01" ou "Janvier 2024"
    private long nombreInscriptions;
}

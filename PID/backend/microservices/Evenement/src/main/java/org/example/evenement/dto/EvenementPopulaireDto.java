package org.example.evenement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvenementPopulaireDto {
    private Long id;
    private String titre;
    private long nombreInscriptions;
}

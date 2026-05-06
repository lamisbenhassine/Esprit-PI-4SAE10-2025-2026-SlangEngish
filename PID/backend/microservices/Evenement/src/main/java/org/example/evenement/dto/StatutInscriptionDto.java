package org.example.evenement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatutInscriptionDto {
    private boolean inscrit;
    private boolean enListeAttente;
    private int positionListeAttente;  // 0 si non en liste
}

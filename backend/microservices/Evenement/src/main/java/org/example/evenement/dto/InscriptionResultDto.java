package org.example.evenement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.evenement.entity.InscriptionEvenement;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InscriptionResultDto {
    private InscriptionEvenement inscription;  // null si liste d'attente
    private boolean listeAttente;
    private Integer position;  // position en liste d'attente si listeAttente=true
}

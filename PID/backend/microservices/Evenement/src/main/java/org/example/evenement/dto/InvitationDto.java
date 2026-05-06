package org.example.evenement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDto {
    private Long inscriptionId;
    private Long evenementId;
    private String titreEvenement;
    private String descriptionEvenement;
    private LocalDate dateEvenement;
    private LocalTime heureEvenement;
    private String lieuEvenement;
    private String typeEvenement;
    private String nomEtudiant;
    private String prenomEtudiant;
    private String emailEtudiant;
}

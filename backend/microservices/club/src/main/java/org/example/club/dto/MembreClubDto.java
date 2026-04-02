package org.example.club.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembreClubDto {
    private Long participationId;
    private Long idEtudiant;
    private String prenomEtudiant;
    private String nomEtudiant;
    private String emailEtudiant;
    private String departementAssigne;
    private String statut;
}


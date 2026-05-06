package org.example.club.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClubMessageDto {
    private Long id;
    private Long idClub;
    private Long idEtudiant;
    private String contenu;
    private String scope;
    private String departement;
    private LocalDateTime dateCreation;
    private String nomEtudiant;
    private String prenomEtudiant;
}

package org.example.club.dto;

import lombok.Data;

@Data
public class FeedbackClubRequestDto {
    private Long idEtudiant;
    private Long idClub;
    private Integer note; // 1..5
    private String commentaire;
}


package com.school.schoolservice.matching.dto;

import lombok.Data;

/**
 * Profil éphémère d'un visiteur pour le matching.
 * Aucune persistance en base : utilisé uniquement comme DTO d'entrée.
 */
@Data
public class VisitorProfileDto {

    // Ville ou région souhaitée
    private String ville;

    // Secteur d'activité ciblé (optionnel)
    private String secteur;

    // Compétences principales sous forme de chaîne séparée par des virgules
    private String competences;

    // Type de contrat souhaité (CDI, CDD, STAGE, ALTERNANCE, FREELANCE...)
    private String typeContrat;

    // Salaire souhaité (annuel ou mensuel selon ton choix métier)
    private Double salaireSouhaite;
}


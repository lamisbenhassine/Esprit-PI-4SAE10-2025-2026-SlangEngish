package org.example.club.dto;

import lombok.Data;

@Data
public class PostAssistantRequestDto {
    /** Idées, mots-clés ou titre bref (3–500 caractères). */
    private String keywords;
    /** Nom du club pour le contexte (optionnel). */
    private String clubNom;
}

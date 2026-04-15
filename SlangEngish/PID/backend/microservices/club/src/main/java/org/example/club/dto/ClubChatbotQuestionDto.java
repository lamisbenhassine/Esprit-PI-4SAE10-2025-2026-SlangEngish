package org.example.club.dto;

import lombok.Data;

@Data
public class ClubChatbotQuestionDto {
    /** Question de l'étudiant (3–500 caractères). */
    private String question;
}

package org.example.club.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Résumé public des avis pour un club (liste des clubs côté étudiant).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClubFeedbackSummaryDto {
    private Long clubId;
    private double moyenne;
    private long nombreAvis;
}

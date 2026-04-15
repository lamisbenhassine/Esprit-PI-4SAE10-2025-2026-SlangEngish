package org.example.evenement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoyenneFeedbackDto {
    private Long evenementId;
    private Double moyenne;
    private Long nombreAvis;
}

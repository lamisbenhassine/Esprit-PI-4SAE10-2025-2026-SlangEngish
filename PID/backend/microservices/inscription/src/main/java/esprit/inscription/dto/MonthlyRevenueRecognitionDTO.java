package esprit.inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Métier avancé 3 : répartition du revenu (revenue recognition) par mois.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenueRecognitionDTO {
    private int year;
    private int month; // 1-12
    private BigDecimal recognizedAmount;
    private long orders; // optional: number of orders contributing to this month
}


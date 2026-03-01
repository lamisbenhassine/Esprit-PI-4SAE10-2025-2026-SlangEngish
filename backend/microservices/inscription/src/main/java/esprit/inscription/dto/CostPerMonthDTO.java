package esprit.inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Métier avancé 1 (style Duolingo) : coût par mois selon la durée du plan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostPerMonthDTO {
    private Long planId;
    private String planName;
    private BigDecimal totalPrice;
    private Integer durationDays;
    private Double durationMonths;
    /** Coût par mois (totalPrice / durationMonths). */
    private BigDecimal costPerMonth;
    private String currency;
}

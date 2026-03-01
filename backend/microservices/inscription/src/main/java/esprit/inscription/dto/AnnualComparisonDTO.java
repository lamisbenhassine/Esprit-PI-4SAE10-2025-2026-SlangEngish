package esprit.inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Métier avancé 1 (style Duolingo) : réduction annuelle (prix mensuel × 12 vs prix annuel).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnualComparisonDTO {
    private Long monthlyPlanId;
    private Long yearlyPlanId;
    private String monthlyPlanName;
    private String yearlyPlanName;
    /** Prix du plan mensuel (pour 1 mois). */
    private BigDecimal monthlyPrice;
    /** Prix du plan annuel (pour 12 mois). */
    private BigDecimal yearlyPrice;
    /** Si on payait 12 × mensuel. */
    private BigDecimal monthlyPriceTimes12;
    /** Économie en montant (monthlyPriceTimes12 - yearlyPrice). */
    private BigDecimal savingsAmount;
    /** Économie en % (savingsAmount / monthlyPriceTimes12 * 100). */
    private BigDecimal savingsPercent;
    /** Coût par mois avec le plan annuel (yearlyPrice / 12). */
    private BigDecimal yearlyCostPerMonth;
    private String currency;
}

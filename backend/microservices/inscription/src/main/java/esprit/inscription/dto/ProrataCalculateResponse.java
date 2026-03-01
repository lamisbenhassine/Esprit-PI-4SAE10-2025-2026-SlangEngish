package esprit.inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Result of prorata calculation: credit for remaining time, new plan prorata cost, amount to pay or refund.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProrataCalculateResponse {
    private String currentPlanName;
    private String newPlanName;
    private String currency;
    private int daysRemaining;
    /** Credit for unused time (value of remaining days on current plan). */
    private BigDecimal credits;
    /** Prorata cost of the new plan for the same remaining period. */
    private BigDecimal newPlanProrataPrice;
    /** Amount to pay now (upgrade). Zero or positive. */
    private BigDecimal amountToPay;
    /** Refund or credit for next invoice (downgrade). Zero or positive. */
    private BigDecimal refund;
}

package esprit.inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Prévisualisation d'une utilisation de points sur une commande :
 * points demandés, points réellement utilisés, réduction obtenue et total final.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyRedemptionPreviewDTO {

    private Long userId;
    private BigDecimal orderTotal;

    private Long requestedPoints;
    private Long appliedPoints;

    private BigDecimal discountAmount;
    private BigDecimal finalTotal;

    /**
     * Plafond théorique pour cette commande (min(solde, 30 % du panier)).
     */
    private BigDecimal maxPossibleDiscount;
}


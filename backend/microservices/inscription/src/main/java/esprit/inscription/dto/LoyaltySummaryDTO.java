package esprit.inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Métier avancé 6 : points de fidélité – résumé du compte et réduction maximale possible.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltySummaryDTO {

    private Long userId;
    private Long balancePoints;
    private Long lifetimePoints;
    /**
     * Palier actuel : BRONZE / SILVER / GOLD.
     */
    private String tier;

    /**
     * Valeur maximale, en montant, que l'utilisateur peut utiliser sur une commande donnée,
     * compte tenu de son solde et du plafond en pourcentage.
     */
    private BigDecimal maxDiscountAmount;

    /**
     * Plafond en pourcentage du panier (ex. 0.30 pour 30 %).
     */
    private BigDecimal maxDiscountPercent;

    /**
     * Montant de réduction obtenu pour 100 points (ex. 5 TND).
     */
    private BigDecimal discountPer100Points;
}


package esprit.inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Réduction pour 2+ frères/sœurs : prix total avec remise, prix par personne.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiblingDiscountDTO {
    private Long planId;
    private String planName;
    private BigDecimal basePricePerPerson;
    private Integer numberOfSiblings;
    /** Pourcentage de réduction appliqué (ex. 10 pour 2 frères/sœurs). */
    private BigDecimal discountPercent;
    /** Montant total de la réduction. */
    private BigDecimal discountAmount;
    /** Prix total après réduction (basePrice × N × (1 - discountPercent/100)). */
    private BigDecimal totalAfterDiscount;
    /** Prix par personne après réduction. */
    private BigDecimal pricePerPersonAfterDiscount;
    private String currency;
}

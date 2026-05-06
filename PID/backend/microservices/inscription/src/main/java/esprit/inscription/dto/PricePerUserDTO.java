package esprit.inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Métier avancé 1 (style Duolingo) : plan famille — prix total / N → prix par utilisateur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricePerUserDTO {
    private Long planId;
    private String planName;
    private BigDecimal totalPrice;
    private Integer numberOfUsers;
    /** totalPrice / numberOfUsers */
    private BigDecimal pricePerUser;
    private String currency;
}

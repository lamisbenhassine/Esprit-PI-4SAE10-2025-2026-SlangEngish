package esprit.inscription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "promo_code")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    /** PERCENTAGE or FIXED_AMOUNT */
    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType = "PERCENTAGE";

    /** For PERCENTAGE: 10 = 10%. For FIXED_AMOUNT: amount in same unit as order (e.g. 15.00) */
    @Column(name = "discount_value", nullable = false)
    private Double discountValue;

    /** Optional: minimum order amount to apply the code */
    @Column(name = "min_purchase_amount")
    private Double minPurchaseAmount;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

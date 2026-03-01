package esprit.inscription.service;

import esprit.inscription.entity.PromoCode;
import esprit.inscription.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    @Transactional
    public PromoCode create(PromoCode promo) {
        if (promo.getCode() == null || promo.getCode().isBlank()) {
            throw new IllegalArgumentException("Code is required");
        }
        promo.setCode(promo.getCode().trim().toUpperCase());
        promo.setUsedCount(promo.getUsedCount() == null ? 0 : promo.getUsedCount());
        return promoCodeRepository.save(promo);
    }

    /**
     * Validates the promo code for the given cart/order amount and returns the discount amount.
     * Does not increment usedCount (that happens when order is created).
     */
    public Optional<PromoCodeValidationResult> validate(String code, BigDecimal orderAmount) {
        if (code == null || code.isBlank() || orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        Optional<PromoCode> opt = promoCodeRepository.findByCodeIgnoreCaseAndActiveTrue(code.trim());
        if (opt.isEmpty()) {
            return Optional.of(PromoCodeValidationResult.invalid("Code promo invalide ou inactif."));
        }
        PromoCode promo = opt.get();
        LocalDateTime now = LocalDateTime.now();
        if (promo.getValidFrom() != null && now.isBefore(promo.getValidFrom())) {
            return Optional.of(PromoCodeValidationResult.invalid("Ce code n'est pas encore valide."));
        }
        if (promo.getValidUntil() != null && now.isAfter(promo.getValidUntil())) {
            return Optional.of(PromoCodeValidationResult.invalid("Ce code a expiré."));
        }
        // Sécuriser usedCount nul (anciens enregistrements) pour éviter les NullPointerException
        int usedCount = promo.getUsedCount() == null ? 0 : promo.getUsedCount();
        if (promo.getMaxUses() != null && usedCount >= promo.getMaxUses()) {
            return Optional.of(PromoCodeValidationResult.invalid("Ce code a atteint son nombre d'utilisations maximal."));
        }
        if (promo.getMinPurchaseAmount() != null && orderAmount.compareTo(promo.getMinPurchaseAmount()) < 0) {
            return Optional.of(PromoCodeValidationResult.invalid(
                "Montant minimum requis: " + String.format("%.2f", promo.getMinPurchaseAmount()) + " €."));
        }
        BigDecimal discountAmount = BigDecimal.valueOf(calculateDiscountAmount(promo, orderAmount));
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.of(PromoCodeValidationResult.invalid("Ce code n'offre aucune réduction pour ce montant."));
        }
        return Optional.of(PromoCodeValidationResult.valid(promo.getCode(), discountAmount));
    }

    /** Call this when an order is successfully created with this promo code. */
    @Transactional
    public void incrementUsage(String code) {
        if (code == null || code.isBlank()) return;
        promoCodeRepository.findByCodeIgnoreCaseAndActiveTrue(code.trim()).ifPresent(p -> {
            p.setUsedCount((p.getUsedCount() == null ? 0 : p.getUsedCount()) + 1);
            promoCodeRepository.save(p);
        });
    }

    private double calculateDiscountAmount(PromoCode promo, BigDecimal orderAmount) {
        if ("FIXED_AMOUNT".equalsIgnoreCase(promo.getDiscountType())) {
            double fixed = promo.getDiscountValue() == null ? 0 : promo.getDiscountValue().doubleValue();
            return Math.min(fixed, orderAmount.doubleValue());
        }
        // PERCENTAGE
        double percentage = promo.getDiscountValue() == null ? 0 : promo.getDiscountValue().doubleValue();
        return orderAmount.doubleValue() * (percentage / 100.0);
    }

    private double computeDiscount(PromoCode promo, BigDecimal orderAmount) {
        return calculateDiscountAmount(promo, orderAmount);
    }

    public List<PromoCode> getAllPromoCodes() {
        return promoCodeRepository.findByActiveTrue();
    }

    public Optional<PromoCode> getPromoCodeById(Long id) {
        return promoCodeRepository.findById(id);
    }

    public void deletePromoCode(Long id) {
        if (!promoCodeRepository.existsById(id)) {
            throw new RuntimeException("Promo code not found: " + id);
        }
        promoCodeRepository.deleteById(id);
    }

    @lombok.Data
    @lombok.AllArgsConstructor(staticName = "of")
    public static class PromoCodeValidationResult {
        private boolean valid;
        private String message;
        private String code;
        private BigDecimal discountAmount;
        private BigDecimal totalAfterDiscount;

        public static PromoCodeValidationResult invalid(String message) {
            return new PromoCodeValidationResult(false, message, null, BigDecimal.ZERO, null);
        }

        public static PromoCodeValidationResult valid(String code, BigDecimal discountAmount) {
            return new PromoCodeValidationResult(true, null, code, discountAmount, null);
        }
    }
}

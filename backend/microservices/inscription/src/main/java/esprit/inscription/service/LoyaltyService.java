package esprit.inscription.service;

import esprit.inscription.dto.LoyaltyRedemptionPreviewDTO;
import esprit.inscription.dto.LoyaltySummaryDTO;
import esprit.inscription.entity.LoyaltyAccount;
import esprit.inscription.entity.LoyaltyTransaction;
import esprit.inscription.repository.LoyaltyAccountRepository;
import esprit.inscription.repository.LoyaltyTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;

/**
 * Métier avancé 6 : gestion des points de fidélité (gain + calcul de réduction).
 *
 * Règles implémentées :
 * - 10 points gagnés par unité de monnaie dépensée (ex. 10 pts / 1 TND).
 * - 100 points = 5 unités de réduction.
 * - Plafond : au maximum 30 % du montant de la commande peuvent être payés en points.
 * - Paliers :
 *   - SILVER si lifetimePoints >= 5000 → +5 % de points
 *   - GOLD   si lifetimePoints >= 15000 → +10 % de points
 */
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository transactionRepository;

    private static final BigDecimal POINTS_PER_UNIT = BigDecimal.valueOf(10);      // 10 pts / 1 TND
    private static final BigDecimal DISCOUNT_PER_100_POINTS = BigDecimal.valueOf(5); // 100 pts = 5 TND
    private static final BigDecimal MAX_DISCOUNT_PERCENT = BigDecimal.valueOf(0.30); // 30 %

    // ─── Lecture / résumé du compte ──────────────────────────────────────────────

    @Transactional
    public LoyaltySummaryDTO getSummary(Long userId, BigDecimal orderTotal) {
        LoyaltyAccount account = getOrCreateAccount(userId);
        String tier = computeTier(account.getLifetimePoints());

        BigDecimal maxDiscountAmount = ZERO;
        if (orderTotal != null && orderTotal.compareTo(ZERO) > 0) {
            maxDiscountAmount = computeMaxDiscountForOrder(orderTotal, account.getBalancePoints());
        }

        return LoyaltySummaryDTO.builder()
                .userId(userId)
                .balancePoints(account.getBalancePoints())
                .lifetimePoints(account.getLifetimePoints())
                .tier(tier)
                .maxDiscountAmount(maxDiscountAmount)
                .maxDiscountPercent(MAX_DISCOUNT_PERCENT)
                .discountPer100Points(DISCOUNT_PER_100_POINTS)
                .build();
    }

    /**
     * Vue admin : liste de tous les comptes de fidélité avec leur palier.
     * Cette méthode ne calcule pas de réduction max (dépend d'une commande),
     * elle renvoie donc uniquement les informations de compte + tier.
     */
    @Transactional(readOnly = true)
    public List<LoyaltySummaryDTO> getAllAccountsSummary() {
        return accountRepository.findAll()
                .stream()
                .map(acc -> LoyaltySummaryDTO.builder()
                        .userId(acc.getUserId())
                        .balancePoints(acc.getBalancePoints())
                        .lifetimePoints(acc.getLifetimePoints())
                        .tier(computeTier(acc.getLifetimePoints()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Seed de comptes de fidélité de démonstration pour le backoffice
     * (ex. différents paliers Bronze / Silver / Gold).
     * Idempotent : ne recrée pas les comptes déjà existants.
     */
    @Transactional
    public List<LoyaltySummaryDTO> seedDemoAccounts() {
        createOrUpdateDemoAccount(1L, 0L, 0L);       // Bronze
        createOrUpdateDemoAccount(2L, 2500L, 6000L); // Silver
        createOrUpdateDemoAccount(3L, 8000L, 16000L); // Gold
        createOrUpdateDemoAccount(4L, 4000L, 10000L); // Silver proche Gold
        return getAllAccountsSummary();
    }

    private void createOrUpdateDemoAccount(Long userId, Long balancePoints, Long lifetimePoints) {
        LoyaltyAccount account = accountRepository.findByUserId(userId)
                .orElseGet(() -> LoyaltyAccount.builder()
                        .userId(userId)
                        .balancePoints(0L)
                        .lifetimePoints(0L)
                        .build());
        account.setBalancePoints(balancePoints);
        account.setLifetimePoints(lifetimePoints);
        accountRepository.save(account);
    }

    /**
     * Applique définitivement un débit de points après paiement confirmé.
     * Crée une transaction REDEEMED et met à jour le solde.
     */
    @Transactional
    public void applyRedemption(Long userId, Long orderId, long appliedPoints) {
        if (appliedPoints <= 0L) {
            return;
        }
        LoyaltyAccount account = getOrCreateAccount(userId);
        Long current = account.getBalancePoints() != null ? account.getBalancePoints() : 0L;
        if (current <= 0L) {
            return;
        }
        long actualApplied = Math.min(current, appliedPoints);
        if (actualApplied <= 0L) {
            return;
        }
        account.setBalancePoints(current - actualApplied);
        accountRepository.save(account);

        LoyaltyTransaction tx = LoyaltyTransaction.builder()
                .userId(userId)
                .orderId(orderId)
                .type(LoyaltyTransaction.Type.REDEEMED)
                .points(actualApplied)
                .build();
        transactionRepository.save(tx);
    }

    // ─── Gain de points après paiement confirmé ─────────────────────────────────

    @Transactional
    public long addPointsForOrder(Long userId, BigDecimal orderAmount, Long orderId) {
        if (orderAmount == null || orderAmount.compareTo(ZERO) <= 0) {
            return 0L;
        }
        LoyaltyAccount account = getOrCreateAccount(userId);
        String tier = computeTier(account.getLifetimePoints());

        // pointsGagnes = floor(montantCommande * pointsParEuro * multiplicateurPalier)
        BigDecimal basePoints = orderAmount.multiply(POINTS_PER_UNIT);
        BigDecimal multiplier = BigDecimal.ONE;
        if ("SILVER".equals(tier)) {
            multiplier = BigDecimal.valueOf(1.05);
        } else if ("GOLD".equals(tier)) {
            multiplier = BigDecimal.valueOf(1.10);
        }
        long earned = basePoints.multiply(multiplier).setScale(0, BigDecimal.ROUND_FLOOR).longValue();
        if (earned <= 0) {
            return 0L;
        }

        account.setBalancePoints(account.getBalancePoints() + earned);
        account.setLifetimePoints(account.getLifetimePoints() + earned);
        accountRepository.save(account);

        LoyaltyTransaction tx = LoyaltyTransaction.builder()
                .userId(userId)
                .orderId(orderId)
                .type(LoyaltyTransaction.Type.EARNED)
                .points(earned)
                .build();
        transactionRepository.save(tx);

        return earned;
    }

    // ─── Prévisualisation d'une réduction par points ────────────────────────────

    @Transactional
    public LoyaltyRedemptionPreviewDTO previewRedemption(Long userId, BigDecimal orderTotal, long requestedPoints) {
        LoyaltyAccount account = getOrCreateAccount(userId);
        if (orderTotal == null || orderTotal.compareTo(ZERO) <= 0 || requestedPoints <= 0) {
            return LoyaltyRedemptionPreviewDTO.builder()
                    .userId(userId)
                    .orderTotal(orderTotal != null ? orderTotal : ZERO)
                    .requestedPoints(requestedPoints)
                    .appliedPoints(0L)
                    .discountAmount(ZERO)
                    .finalTotal(orderTotal != null ? orderTotal : ZERO)
                    .maxPossibleDiscount(computeMaxDiscountForOrder(orderTotal != null ? orderTotal : ZERO,
                            account.getBalancePoints()))
                    .build();
        }

        BigDecimal maxDiscount = computeMaxDiscountForOrder(orderTotal, account.getBalancePoints());

        // Réduction théorique correspondant aux points demandés : (N / 100) * 5
        BigDecimal requestedDiscount = BigDecimal.valueOf(requestedPoints)
                .divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_FLOOR)
                .multiply(DISCOUNT_PER_100_POINTS);

        BigDecimal discount = requestedDiscount.min(maxDiscount);
        if (discount.compareTo(ZERO) <= 0) {
            return LoyaltyRedemptionPreviewDTO.builder()
                    .userId(userId)
                    .orderTotal(orderTotal)
                    .requestedPoints(requestedPoints)
                    .appliedPoints(0L)
                    .discountAmount(ZERO)
                    .finalTotal(orderTotal)
                    .maxPossibleDiscount(maxDiscount)
                    .build();
        }

        // Points réellement utilisés correspondant à la réduction (on arrondit à l'entier inférieur de 100 pts).
        long appliedPoints = discount
                .divide(DISCOUNT_PER_100_POINTS, 4, BigDecimal.ROUND_FLOOR)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, BigDecimal.ROUND_FLOOR)
                .longValue();

        if (appliedPoints > account.getBalancePoints()) {
            appliedPoints = account.getBalancePoints();
        }

        BigDecimal finalTotal = orderTotal.subtract(discount);
        if (finalTotal.compareTo(ZERO) < 0) {
            finalTotal = ZERO;
        }

        return LoyaltyRedemptionPreviewDTO.builder()
                .userId(userId)
                .orderTotal(orderTotal)
                .requestedPoints(requestedPoints)
                .appliedPoints(appliedPoints)
                .discountAmount(discount)
                .finalTotal(finalTotal)
                .maxPossibleDiscount(maxDiscount)
                .build();
    }

    // ─── Helpers internes ───────────────────────────────────────────────────────

    private LoyaltyAccount getOrCreateAccount(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    LoyaltyAccount acc = LoyaltyAccount.builder()
                            .userId(userId)
                            .balancePoints(0L)
                            .lifetimePoints(0L)
                            .build();
                    return accountRepository.save(acc);
                });
    }

    private String computeTier(Long lifetimePoints) {
        long pts = lifetimePoints != null ? lifetimePoints : 0L;
        if (pts >= 15000L) return "GOLD";
        if (pts >= 5000L) return "SILVER";
        return "BRONZE";
    }

    private BigDecimal computeMaxDiscountForOrder(BigDecimal orderTotal, Long balancePoints) {
        if (orderTotal == null || orderTotal.compareTo(ZERO) <= 0 || balancePoints == null || balancePoints <= 0L) {
            return ZERO;
        }
        // Valeur max liée au solde : (points / 100) * 5
        BigDecimal byBalance = BigDecimal.valueOf(balancePoints)
                .divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_FLOOR)
                .multiply(DISCOUNT_PER_100_POINTS);

        // Plafond 30 % du panier
        BigDecimal byPercent = orderTotal.multiply(MAX_DISCOUNT_PERCENT);

        return byBalance.min(byPercent);
    }
}


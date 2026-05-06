package esprit.inscription.service;

import esprit.inscription.dto.AnnualComparisonDTO;
import esprit.inscription.dto.CostPerMonthDTO;
import esprit.inscription.dto.PricePerUserDTO;
import esprit.inscription.dto.RecommendationDTO;
import esprit.inscription.dto.SiblingDiscountDTO;
import esprit.inscription.entity.SubscriptionPlan;
import esprit.inscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Métier avancé 1 (style Duolingo) : calculs de tarification.
 * - Coût par mois selon la durée du plan
 * - Réduction annuelle (mensuel × 12 vs annuel)
 * - Plan famille : prix par utilisateur (prix total / N)
 * - Scoring "qui devrait s'abonner" (recommandation)
 */
@Service
@RequiredArgsConstructor
public class SubscriptionPricingService {

    private static final int SCALE = 2;
    private static final BigDecimal MONTH_DAYS = new BigDecimal("30");
    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    /**
     * Coût par mois : price / (durationDays / 30).
     */
    public Optional<CostPerMonthDTO> getCostPerMonth(Long planId) {
        return subscriptionPlanRepository.findById(planId)
                .map(this::computeCostPerMonth);
    }

    /** Coût par mois pour tous les plans (pour affichage liste). */
    public List<CostPerMonthDTO> getCostPerMonthAll() {
        return subscriptionPlanRepository.findAll().stream()
                .map(this::computeCostPerMonth)
                .filter(dto -> dto != null)
                .toList();
    }

    public CostPerMonthDTO computeCostPerMonth(SubscriptionPlan plan) {
        if (plan == null || plan.getPrice() == null || plan.getDurationDays() == null || plan.getDurationDays() <= 0) {
            return null;
        }
        BigDecimal price = plan.getPrice();
        BigDecimal durationDays = BigDecimal.valueOf(plan.getDurationDays());
        BigDecimal durationMonths = durationDays.divide(MONTH_DAYS, 4, RoundingMode.HALF_UP);
        BigDecimal costPerMonth = price.divide(durationMonths, SCALE, RoundingMode.HALF_UP);
        return CostPerMonthDTO.builder()
                .planId(plan.getId())
                .planName(plan.getName())
                .totalPrice(price)
                .durationDays(plan.getDurationDays())
                .durationMonths(durationMonths.doubleValue())
                .costPerMonth(costPerMonth)
                .currency(plan.getCurrency() != null ? plan.getCurrency() : "TND")
                .build();
    }

    /**
     * Comparaison annuelle : plan mensuel vs plan annuel → économie en montant et %.
     */
    public Optional<AnnualComparisonDTO> compareAnnual(Long monthlyPlanId, Long yearlyPlanId) {
        Optional<SubscriptionPlan> monthly = subscriptionPlanRepository.findById(monthlyPlanId);
        Optional<SubscriptionPlan> yearly = subscriptionPlanRepository.findById(yearlyPlanId);
        if (monthly.isEmpty() || yearly.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(computeAnnualComparison(monthly.get(), yearly.get()));
    }

    public AnnualComparisonDTO computeAnnualComparison(SubscriptionPlan monthlyPlan, SubscriptionPlan yearlyPlan) {
        BigDecimal monthlyPrice = monthlyPlan.getPrice() != null ? monthlyPlan.getPrice() : BigDecimal.ZERO;
        BigDecimal yearlyPrice = yearlyPlan.getPrice() != null ? yearlyPlan.getPrice() : BigDecimal.ZERO;
        BigDecimal monthlyTimes12 = monthlyPrice.multiply(TWELVE).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal savingsAmount = monthlyTimes12.subtract(yearlyPrice).max(BigDecimal.ZERO);
        BigDecimal savingsPercent = BigDecimal.ZERO;
        if (monthlyTimes12.compareTo(BigDecimal.ZERO) > 0) {
            savingsPercent = savingsAmount.multiply(HUNDRED).divide(monthlyTimes12, SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal yearlyCostPerMonth = yearlyPrice.compareTo(BigDecimal.ZERO) > 0
                ? yearlyPrice.divide(TWELVE, SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String currency = yearlyPlan.getCurrency() != null ? yearlyPlan.getCurrency() : "TND";
        return AnnualComparisonDTO.builder()
                .monthlyPlanId(monthlyPlan.getId())
                .yearlyPlanId(yearlyPlan.getId())
                .monthlyPlanName(monthlyPlan.getName())
                .yearlyPlanName(yearlyPlan.getName())
                .monthlyPrice(monthlyPrice)
                .yearlyPrice(yearlyPrice)
                .monthlyPriceTimes12(monthlyTimes12)
                .savingsAmount(savingsAmount)
                .savingsPercent(savingsPercent)
                .yearlyCostPerMonth(yearlyCostPerMonth)
                .currency(currency)
                .build();
    }

    /**
     * Plan famille : prix par utilisateur = totalPrice / numberOfUsers.
     */
    public Optional<PricePerUserDTO> getPricePerUser(Long planId, int numberOfUsers) {
        if (numberOfUsers <= 0) {
            return Optional.empty();
        }
        return subscriptionPlanRepository.findById(planId)
                .map(plan -> computePricePerUser(plan, numberOfUsers));
    }

    public PricePerUserDTO computePricePerUser(SubscriptionPlan plan, int numberOfUsers) {
        if (plan == null || plan.getPrice() == null) {
            return null;
        }
        BigDecimal total = plan.getPrice();
        BigDecimal perUser = total.divide(BigDecimal.valueOf(numberOfUsers), SCALE, RoundingMode.HALF_UP);
        return PricePerUserDTO.builder()
                .planId(plan.getId())
                .planName(plan.getName())
                .totalPrice(total)
                .numberOfUsers(numberOfUsers)
                .pricePerUser(perUser)
                .currency(plan.getCurrency() != null ? plan.getCurrency() : "TND")
                .build();
    }

    /**
     * Recommandation (scoring) : selon minutes/jour, niveau, objectif.
     * Score 0-100 + message.
     */
    public RecommendationDTO getRecommendation(Integer minutesPerDay, String level, String goal) {
        int score = 50;
        StringBuilder message = new StringBuilder();

        if (minutesPerDay != null) {
            if (minutesPerDay >= 20) {
                score = Math.min(100, score + 30);
                message.append("Idéal : vous étudiez ").append(minutesPerDay).append(" min/jour. ");
            } else if (minutesPerDay >= 10) {
                score = Math.min(95, score + 15);
                message.append("Un abonnement vous convient : ").append(minutesPerDay).append(" min/jour. ");
            } else if (minutesPerDay != null && minutesPerDay > 0) {
                score = Math.max(0, score - 10);
                message.append("Recommandé si vous pouvez consacrer au moins 15–20 min/jour. ");
            }
        }

        if (level != null && !level.isBlank()) {
            message.append("Niveau ").append(level.trim().toUpperCase()).append(". ");
        }
        if (goal != null && !goal.isBlank()) {
            message.append("Objectif : ").append(goal.trim()).append(". ");
        }
        if (message.length() == 0) {
            message.append("Indiquez votre temps d'étude et votre niveau pour une recommandation personnalisée.");
        }

        Optional<SubscriptionPlan> recommendedPlan = Optional.empty();
        if (level != null && !level.isBlank()) {
            String levelKey = level.trim().toUpperCase();
            List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();

            // 1. Try exact match (e.g. C2 -> planType C2)
            for (SubscriptionPlan p : plans) {
                if (p.getPlanType() != null && levelKey.equals(p.getPlanType().toUpperCase())) {
                    recommendedPlan = Optional.of(p);
                    break;
                }
            }

            // 2. Fallback: if level is C2 and no exact plan exists, try C1
            if (recommendedPlan.isEmpty() && "C2".equals(levelKey)) {
                for (SubscriptionPlan p : plans) {
                    if (p.getPlanType() != null && "C1".equalsIgnoreCase(p.getPlanType())) {
                        recommendedPlan = Optional.of(p);
                        break;
                    }
                }
            }
        }

        return RecommendationDTO.builder()
                .score(Math.min(100, Math.max(0, score)))
                .message(message.toString().trim())
                .suggestedLevel(level != null ? level.trim() : null)
                .recommendedPlanId(recommendedPlan.map(SubscriptionPlan::getId).orElse(null))
                .recommendedPlanName(recommendedPlan.map(SubscriptionPlan::getName).orElse(null))
                .build();
    }

    /**
     * Réduction frères/sœurs : 2 inscrits = 10 % de réduction, 3 = 12 %, 4+ = 15 % sur le total.
     */
    public Optional<SiblingDiscountDTO> getSiblingDiscount(Long planId, int numberOfSiblings) {
        if (numberOfSiblings < 2) {
            return Optional.empty();
        }
        return subscriptionPlanRepository.findById(planId)
                .map(plan -> computeSiblingDiscount(plan, numberOfSiblings));
    }

    public SiblingDiscountDTO computeSiblingDiscount(SubscriptionPlan plan, int numberOfSiblings) {
        if (plan == null || numberOfSiblings < 2) {
            return null;
        }
        // Prix du plan ; si absent en base, utiliser un défaut pour que le métier reste utilisable
        BigDecimal basePrice = plan.getPrice() != null && plan.getPrice().compareTo(BigDecimal.ZERO) > 0
                ? plan.getPrice()
                : new BigDecimal("49.00");
        int percent;
        if (numberOfSiblings >= 4) {
            percent = 15;
        } else if (numberOfSiblings == 3) {
            percent = 12;
        } else {
            percent = 10;
        }
        BigDecimal totalBase = basePrice.multiply(BigDecimal.valueOf(numberOfSiblings)).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal discountPercent = BigDecimal.valueOf(percent);
        BigDecimal discountAmount = totalBase.multiply(discountPercent).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
        BigDecimal totalAfterDiscount = totalBase.subtract(discountAmount).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal pricePerPersonAfterDiscount = totalAfterDiscount.divide(BigDecimal.valueOf(numberOfSiblings), SCALE, RoundingMode.HALF_UP);
        String currency = plan.getCurrency() != null ? plan.getCurrency() : "TND";
        return SiblingDiscountDTO.builder()
                .planId(plan.getId())
                .planName(plan.getName())
                .basePricePerPerson(basePrice)
                .numberOfSiblings(numberOfSiblings)
                .discountPercent(discountPercent)
                .discountAmount(discountAmount)
                .totalAfterDiscount(totalAfterDiscount)
                .pricePerPersonAfterDiscount(pricePerPersonAfterDiscount)
                .currency(currency)
                .build();
    }

    /**
     * Trouve un plan mensuel et un plan annuel pour afficher la comparaison (par type).
     */
    public Optional<AnnualComparisonDTO> findAndCompareAnnual() {
        List<SubscriptionPlan> monthly = subscriptionPlanRepository.findByPlanType("monthly");
        List<SubscriptionPlan> yearly = subscriptionPlanRepository.findByPlanType("yearly");
        if (monthly.isEmpty() || yearly.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(computeAnnualComparison(monthly.get(0), yearly.get(0)));
    }
}

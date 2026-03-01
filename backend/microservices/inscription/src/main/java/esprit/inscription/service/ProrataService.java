package esprit.inscription.service;

import esprit.inscription.dto.ProrataCalculateRequest;
import esprit.inscription.dto.ProrataCalculateResponse;
import esprit.inscription.entity.SubscriptionPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Métier avancé 2: Prorata upgrade/downgrade.
 * Credit = (pricePaid / totalDays) × daysRemaining.
 * New plan prorata = (newPlanPrice / newPlanDurationDays) × daysRemaining.
 * amountToPay = max(0, newPlanProrata - credits); refund = max(0, credits - newPlanProrata).
 */
@Service
@RequiredArgsConstructor
public class ProrataService {

    private final SubscriptionPlanService subscriptionPlanService;

    public ProrataCalculateResponse calculateChange(ProrataCalculateRequest req) {
        if (req == null || req.getCurrentPlanId() == null || req.getNewPlanId() == null
                || req.getPricePaid() == null || req.getDurationDays() == null || req.getSubscriptionStartDate() == null) {
            throw new IllegalArgumentException("Missing required fields: currentPlanId, newPlanId, pricePaid, durationDays, subscriptionStartDate");
        }
        LocalDate changeDate = req.getChangeDate() != null ? req.getChangeDate() : LocalDate.now();
        LocalDate endDate = req.getSubscriptionStartDate().plusDays(req.getDurationDays());
        int daysRemaining = (int) ChronoUnit.DAYS.between(changeDate, endDate);
        if (daysRemaining < 0) daysRemaining = 0;

        BigDecimal pricePaid = req.getPricePaid();
        int totalDays = Math.max(1, req.getDurationDays());
        BigDecimal credits = pricePaid.multiply(BigDecimal.valueOf(daysRemaining))
                .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);

        SubscriptionPlan newPlan = subscriptionPlanService.getPlanById(req.getNewPlanId())
                .orElseThrow(() -> new IllegalArgumentException("New plan not found: " + req.getNewPlanId()));
        BigDecimal newPrice = newPlan.getPrice() != null ? newPlan.getPrice() : BigDecimal.ZERO;
        int newDurationDays = newPlan.getDurationDays() != null && newPlan.getDurationDays() > 0
                ? newPlan.getDurationDays() : 30;
        BigDecimal newPlanProrataPrice = newPrice.multiply(BigDecimal.valueOf(daysRemaining))
                .divide(BigDecimal.valueOf(newDurationDays), 2, RoundingMode.HALF_UP);

        BigDecimal amountToPay = newPlanProrataPrice.subtract(credits).setScale(2, RoundingMode.HALF_UP);
        BigDecimal refund = BigDecimal.ZERO;
        if (amountToPay.compareTo(BigDecimal.ZERO) < 0) {
            refund = amountToPay.negate();
            amountToPay = BigDecimal.ZERO;
        }

        String currentPlanName = subscriptionPlanService.getPlanById(req.getCurrentPlanId())
                .map(SubscriptionPlan::getName)
                .orElse("Plan " + req.getCurrentPlanId());
        String currency = newPlan.getCurrency() != null ? newPlan.getCurrency() : "TND";

        return ProrataCalculateResponse.builder()
                .currentPlanName(currentPlanName)
                .newPlanName(newPlan.getName() != null ? newPlan.getName() : newPlan.getPlanType())
                .currency(currency)
                .daysRemaining(daysRemaining)
                .credits(credits)
                .newPlanProrataPrice(newPlanProrataPrice)
                .amountToPay(amountToPay)
                .refund(refund)
                .build();
    }
}

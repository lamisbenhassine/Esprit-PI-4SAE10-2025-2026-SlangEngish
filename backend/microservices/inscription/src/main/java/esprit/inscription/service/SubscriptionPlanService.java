package esprit.inscription.service;

import esprit.inscription.entity.SubscriptionPlan;
import esprit.inscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public List<SubscriptionPlan> getAllPlans() {
        return subscriptionPlanRepository.findAll()
                .stream()
                .map(this::applyDefaultsForResponse)
                .toList();
    }

    public Optional<SubscriptionPlan> getPlanById(Long id) {
        return subscriptionPlanRepository.findById(id)
                .map(this::applyDefaultsForResponse);
    }

    public List<SubscriptionPlan> getPlansByUserId(Long userId) {
        return subscriptionPlanRepository.findByUserId(userId);
    }

    public List<SubscriptionPlan> getPlansByType(String planType) {
        return subscriptionPlanRepository.findByPlanType(planType);
    }

    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        applyDefaultsBeforeSave(plan);
        return subscriptionPlanRepository.save(plan);
    }

    public SubscriptionPlan updatePlan(Long id, SubscriptionPlan updatedPlan) {
        return subscriptionPlanRepository.findById(id)
                .map(plan -> {
                    plan.setPlanType(updatedPlan.getPlanType());
                    plan.setName(updatedPlan.getName());
                    plan.setPrice(updatedPlan.getPrice());
                    plan.setCurrency(updatedPlan.getCurrency());
                    plan.setDurationDays(updatedPlan.getDurationDays());
                    plan.setDescription(updatedPlan.getDescription());
                    plan.setDate(updatedPlan.getDate());
                    plan.setUserId(updatedPlan.getUserId());
                    plan.setCourseId(updatedPlan.getCourseId());
                    plan.setImageUrl(updatedPlan.getImageUrl());
                    applyDefaultsBeforeSave(plan);
                    return subscriptionPlanRepository.save(plan);
                })
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + id));
    }

    /**
     * If some fields are missing in DB (old rows) or not provided by client, set sensible defaults.
     * This guarantees the UI does not show 0 for price/duration.
     */
    private SubscriptionPlan applyDefaultsForResponse(SubscriptionPlan plan) {
        if (plan == null) return null;
        // Do NOT persist changes here; only for response.
        if (plan.getName() == null || plan.getName().isBlank()) {
            plan.setName(plan.getPlanType());
        }
        if (plan.getCurrency() == null || plan.getCurrency().isBlank()) {
            plan.setCurrency("TND");
        }
        if (plan.getDurationDays() == null || plan.getDurationDays() <= 0) {
            plan.setDurationDays(defaultDuration(plan.getPlanType()));
        }
        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            plan.setPrice(defaultPrice(plan.getPlanType()));
        }
        return plan;
    }

    private void applyDefaultsBeforeSave(SubscriptionPlan plan) {
        if (plan == null) return;
        if (plan.getName() == null || plan.getName().isBlank()) {
            plan.setName(plan.getPlanType());
        }
        if (plan.getCurrency() == null || plan.getCurrency().isBlank()) {
            plan.setCurrency("TND");
        }
        if (plan.getDurationDays() == null || plan.getDurationDays() <= 0) {
            plan.setDurationDays(defaultDuration(plan.getPlanType()));
        }
        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            plan.setPrice(defaultPrice(plan.getPlanType()));
        }
    }

    // Use classic switch for compatibility with older Java versions (no switch expressions).
    private int defaultDuration(String planType) {
        if (planType == null) {
            return 30;
        }
        String key = planType.trim().toLowerCase();
        switch (key) {
            case "monthly":
                return 30;
            case "quarterly":
                return 90;
            case "yearly":
                return 365;
            case "premium":
                return 180;
            default:
                return 30;
        }
    }

    private BigDecimal defaultPrice(String planType) {
        if (planType == null) {
            return new BigDecimal("29.99");
        }
        String key = planType.trim().toLowerCase();
        switch (key) {
            case "monthly":
                return new BigDecimal("29.99");
            case "quarterly":
                return new BigDecimal("79.99");
            case "yearly":
                return new BigDecimal("249.99");
            case "premium":
                return new BigDecimal("399.99");
            default:
                return new BigDecimal("29.99");
        }
    }

    public void deletePlan(Long id) {
        subscriptionPlanRepository.deleteById(id);
    }

    /**
     * Crée les offres par défaut si la table est vide. Retourne la liste des plans créés (ou vide si déjà des données).
     */
    public List<SubscriptionPlan> seedDefaultPlansIfEmpty() {
        if (subscriptionPlanRepository.count() > 0) {
            return List.of();
        }
        List<SubscriptionPlan> defaults = List.of(
                plan("Monthly", "Mensuel", "29.99", 30, "Abonnement mensuel"),
                plan("Quarterly", "Trimestriel", "79.99", 90, "Abonnement 3 mois"),
                plan("Yearly", "Annuel", "249.99", 365, "Abonnement 1 an"),
                plan("C1", "Niveau C1", "199.99", 180, "Plan niveau C1"),
                plan("C2", "Niveau C2", "299.99", 365, "Plan niveau C2"),
                plan("Premium", "Premium", "399.99", 365, "Offre premium")
        );
        List<SubscriptionPlan> saved = new ArrayList<>();
        for (SubscriptionPlan p : defaults) {
            saved.add(subscriptionPlanRepository.save(p));
        }
        return saved;
    }

    private static SubscriptionPlan plan(String planType, String name, String price, int durationDays, String description) {
        return SubscriptionPlan.builder()
                .planType(planType)
                .name(name)
                .price(new BigDecimal(price))
                .currency("TND")
                .durationDays(durationDays)
                .description(description)
                .date(LocalDate.now())
                .build();
    }
}

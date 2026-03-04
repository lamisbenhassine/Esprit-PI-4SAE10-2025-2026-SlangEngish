package esprit.inscription.controller;

import esprit.inscription.dto.AnnualComparisonDTO;
import esprit.inscription.dto.CostPerMonthDTO;
import esprit.inscription.dto.PricePerUserDTO;
import esprit.inscription.dto.RecommendationDTO;
import esprit.inscription.dto.SiblingDiscountDTO;
import esprit.inscription.entity.SubscriptionPlan;
import esprit.inscription.service.SubscriptionPlanService;
import esprit.inscription.service.SubscriptionPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inscription/plans")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionPricingService subscriptionPricingService;

    @GetMapping
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        List<SubscriptionPlan> plans = subscriptionPlanService.getAllPlans();
        plans.forEach(plan -> {
            System.out.println("Plan ID: " + plan.getId() + ", Type: " + plan.getPlanType() + ", ImageUrl: " + plan.getImageUrl());
        });
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlan> getPlanById(@PathVariable("id") Long id) {
        return subscriptionPlanService.getPlanById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubscriptionPlan>> getPlansByUserId(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(subscriptionPlanService.getPlansByUserId(userId));
    }

    @GetMapping("/type/{planType}")
    public ResponseEntity<List<SubscriptionPlan>> getPlansByType(@PathVariable("planType") String planType) {
        return ResponseEntity.ok(subscriptionPlanService.getPlansByType(planType));
    }

    @PostMapping("/seed")
    public ResponseEntity<List<SubscriptionPlan>> seedDefaultPlans() {
        List<SubscriptionPlan> created = subscriptionPlanService.seedDefaultPlansIfEmpty();
        return ResponseEntity.ok(created);
    }

    @PostMapping
    public ResponseEntity<SubscriptionPlan> createPlan(@RequestBody SubscriptionPlan plan) {
        System.out.println("Creating plan with imageUrl: " + plan.getImageUrl());
        SubscriptionPlan created = subscriptionPlanService.createPlan(plan);
        System.out.println("Created plan imageUrl: " + created.getImageUrl());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionPlan> updatePlan(@PathVariable("id") Long id,
                                                       @RequestBody SubscriptionPlan plan) {
        try {
            System.out.println("Updating plan " + id + " with imageUrl: " + plan.getImageUrl());
            SubscriptionPlan updated = subscriptionPlanService.updatePlan(id, plan);
            System.out.println("Updated plan imageUrl: " + updated.getImageUrl());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable("id") Long id) {
        subscriptionPlanService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Métier avancé 1 (style Duolingo) : calculs de tarification ─────────────────

    /** Coût par mois selon la durée du plan : price / (durationDays/30). */
    @GetMapping("/{id}/cost-per-month")
    public ResponseEntity<CostPerMonthDTO> getCostPerMonth(@PathVariable("id") Long id) {
        return subscriptionPricingService.getCostPerMonth(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Coût par mois pour tous les plans (batch, pour la liste d'offres). */
    @GetMapping("/cost-per-month-all")
    public ResponseEntity<List<CostPerMonthDTO>> getCostPerMonthAll() {
        return ResponseEntity.ok(subscriptionPricingService.getCostPerMonthAll());
    }

    /** Comparaison annuelle : mensuel × 12 vs prix annuel → économie. */
    @GetMapping("/compare-annual")
    public ResponseEntity<AnnualComparisonDTO> compareAnnual(
            @RequestParam("monthlyPlanId") Long monthlyPlanId,
            @RequestParam("yearlyPlanId") Long yearlyPlanId) {
        return subscriptionPricingService.compareAnnual(monthlyPlanId, yearlyPlanId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Comparaison annuelle auto : trouve un plan monthly et un yearly et les compare. */
    @GetMapping("/compare-annual-auto")
    public ResponseEntity<AnnualComparisonDTO> compareAnnualAuto() {
        return subscriptionPricingService.findAndCompareAnnual()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Plan famille : prix par utilisateur = totalPrice / users. */
    @GetMapping("/{id}/price-per-user")
    public ResponseEntity<PricePerUserDTO> getPricePerUser(
            @PathVariable("id") Long id,
            @RequestParam(value = "users", defaultValue = "6") int users) {
        return subscriptionPricingService.getPricePerUser(id, users)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Réduction frères/sœurs : 2 inscrits = 10 % de réduction sur le total. */
    @GetMapping("/{id}/sibling-discount")
    public ResponseEntity<SiblingDiscountDTO> getSiblingDiscount(
            @PathVariable("id") Long id,
            @RequestParam(value = "siblings", defaultValue = "2") int siblings) {
        return subscriptionPricingService.getSiblingDiscount(id, siblings)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Recommandation (scoring) : qui devrait s'abonner selon temps d'étude, niveau, objectif. */
    @GetMapping("/recommendation")
    public ResponseEntity<RecommendationDTO> getRecommendation(
            @RequestParam(value = "minutesPerDay", required = false) Integer minutesPerDay,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "goal", required = false) String goal) {
        return ResponseEntity.ok(subscriptionPricingService.getRecommendation(minutesPerDay, level, goal));
    }
}

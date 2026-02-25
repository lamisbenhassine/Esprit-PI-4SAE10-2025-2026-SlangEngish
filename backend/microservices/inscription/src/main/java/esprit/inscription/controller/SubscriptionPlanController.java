package esprit.inscription.controller;

import esprit.inscription.entity.SubscriptionPlan;
import esprit.inscription.service.SubscriptionPlanService;
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
}

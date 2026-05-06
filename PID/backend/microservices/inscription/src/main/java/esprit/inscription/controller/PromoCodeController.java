package esprit.inscription.controller;

import esprit.inscription.entity.PromoCode;
import esprit.inscription.service.PromoCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/inscription/promo")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @GetMapping("/validate")
    public ResponseEntity<PromoCodeService.PromoCodeValidationResult> validate(
            @RequestParam("code") String code,
            @RequestParam("amount") BigDecimal amount) {
        log.info("Validating promo code: {} for amount: {}", code, amount);
        return promoCodeService.validate(code, amount)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(PromoCodeService.PromoCodeValidationResult.invalid("Code invalide.")));
    }

    @GetMapping
    public ResponseEntity<List<PromoCode>> getAllPromoCodes() {
        log.info("Getting all promo codes");
        try {
            List<PromoCode> promoCodes = promoCodeService.getAllPromoCodes();
            return ResponseEntity.ok(promoCodes);
        } catch (Exception e) {
            log.error("Error getting all promo codes", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromoCode> getPromoCodeById(@PathVariable Long id) {
        log.info("Getting promo code by ID: {}", id);
        try {
            return promoCodeService.getPromoCodeById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting promo code by ID: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /** Create a promo code (admin / dev). In production, secure this endpoint. */
    @PostMapping
    public ResponseEntity<PromoCode> create(@RequestBody PromoCode promo) {
        log.info("Creating new promo code: {}", promo.getCode());
        try {
            PromoCode createdPromo = promoCodeService.create(promo);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPromo);
        } catch (Exception e) {
            log.error("Error creating promo code", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromoCode(@PathVariable Long id) {
        log.info("Deleting promo code: {}", id);
        try {
            promoCodeService.deletePromoCode(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting promo code: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/create-test-code")
    public ResponseEntity<PromoCode> createTestCode() {
        log.info("Creating test promo code '123'");
        try {
            PromoCode testPromo = PromoCode.builder()
                    .code("123")
                    .discountType("PERCENTAGE")
                    .discountValue(new BigDecimal("10")) // 10% de réduction
                    .minPurchaseAmount(new BigDecimal("10")) // Minimum 10€
                    .maxUses(100)
                    .active(true)
                    .validFrom(java.time.LocalDateTime.now())
                    .validUntil(java.time.LocalDateTime.now().plusMonths(6)) // Valable 6 mois
                    .build();
            
            PromoCode created = promoCodeService.create(testPromo);
            log.info("Test promo code created successfully: {}", created.getCode());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating test promo code", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/test-123")
    public ResponseEntity<PromoCodeService.PromoCodeValidationResult> testCode123() {
        log.info("Testing promo code '123' with amount 50€");
        try {
            return promoCodeService.validate("123", new BigDecimal("50"))
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.ok(PromoCodeService.PromoCodeValidationResult.invalid("Code 123 non trouvé.")));
        } catch (Exception e) {
            log.error("Error testing promo code 123", e);
            return ResponseEntity.badRequest().build();
        }
    }
}

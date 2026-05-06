package esprit.inscription.controller;

import esprit.inscription.entity.PromoCode;
import esprit.inscription.service.PromoCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/inscription/test")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class PromoTestController {

    private final PromoCodeService promoCodeService;

    @GetMapping("/promo-full-test")
    public ResponseEntity<String> runFullPromoTest() {
        log.info("=== DÉBUT TEST AUTOMATIQUE CODE PROMO ===");
        
        try {
            // Étape 1: Créer le code promo "123"
            log.info("Étape 1: Création du code promo '123'");
            PromoCode testPromo = PromoCode.builder()
                    .code("123")
                    .discountType("PERCENTAGE")
                    .discountValue(new BigDecimal("10")) // 10% de réduction
                    .minPurchaseAmount(new BigDecimal("10")) // Minimum 10€
                    .maxUses(100)
                    .active(true)
                    .validFrom(java.time.LocalDateTime.now())
                    .validUntil(java.time.LocalDateTime.now().plusMonths(6))
                    .build();
            
            PromoCode created = promoCodeService.create(testPromo);
            log.info("✅ Code promo créé: {}", created.getCode());
            
            // Étape 2: Tester avec montant valide (50€)
            log.info("Étape 2: Test avec montant valide (50€)");
            var result1 = promoCodeService.validate("123", new BigDecimal("50"));
            if (result1.isPresent()) {
                var validation = result1.get();
                if (validation.isValid()) {
                    log.info("✅ Test 50€ RÉUSSI: {}% de réduction = {}€", 
                            "10", validation.getDiscountAmount());
                } else {
                    log.warn("❌ Test 50€ ÉCHOUÉ: {}", validation.getMessage());
                }
            } else {
                log.warn("❌ Test 50€: Pas de résultat retourné");
            }
            
            // Étape 3: Tester avec montant trop bas (5€)
            log.info("Étape 3: Test avec montant trop bas (5€)");
            var result2 = promoCodeService.validate("123", new BigDecimal("5"));
            if (result2.isPresent()) {
                var validation = result2.get();
                if (validation.isValid()) {
                    log.warn("❌ Test 5€ INATTENDU: Code accepté alors que montant trop bas");
                } else {
                    log.info("✅ Test 5€ RÉUSSI: Rejeté correctement - {}", validation.getMessage());
                }
            }
            
            // Étape 4: Tester avec code invalide
            log.info("Étape 4: Test avec code invalide");
            var result3 = promoCodeService.validate("999", new BigDecimal("50"));
            if (result3.isPresent()) {
                var validation = result3.get();
                if (!validation.isValid()) {
                    log.info("✅ Test code invalide RÉUSSI: {}", validation.getMessage());
                } else {
                    log.warn("❌ Test code invalide ÉCHOUÉ: Code accepté alors qu'il n'existe pas");
                }
            }
            
            // Étape 5: Tester avec montant exact (10€)
            log.info("Étape 5: Test avec montant minimum exact (10€)");
            var result4 = promoCodeService.validate("123", new BigDecimal("10"));
            if (result4.isPresent()) {
                var validation = result4.get();
                if (validation.isValid()) {
                    log.info("✅ Test 10€ RÉUSSI: Réduction de {}€", validation.getDiscountAmount());
                } else {
                    log.warn("❌ Test 10€ ÉCHOUÉ: {}", validation.getMessage());
                }
            }
            
            log.info("=== FIN TEST AUTOMATIQUE ===");
            
            return ResponseEntity.ok("""
                Test automatique terminé! Regardez les logs du backend pour les détails.
                
                Résultats attendus:
                - Code "123" créé avec succès
                - 50€ → 45€ (10% de réduction)
                - 5€ → Rejeté (montant minimum 10€)
                - Code "999" → Rejeté (inexistant)
                - 10€ → 9€ (10% de réduction)
                """);
                
        } catch (Exception e) {
            log.error("❌ ERREUR PENDANT LE TEST AUTOMATIQUE", e);
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }
    
    @GetMapping("/promo-simple-test")
    public ResponseEntity<String> runSimpleTest() {
        log.info("=== TEST SIMPLE CODE PROMO ===");
        
        try {
            // Créer le code s'il n'existe pas
            PromoCode testPromo = PromoCode.builder()
                    .code("123")
                    .discountType("PERCENTAGE")
                    .discountValue(new BigDecimal("10"))
                    .minPurchaseAmount(new BigDecimal("10"))
                    .maxUses(100)
                    .active(true)
                    .validFrom(java.time.LocalDateTime.now())
                    .validUntil(java.time.LocalDateTime.now().plusMonths(6))
                    .build();
            
            promoCodeService.create(testPromo);
            
            // Tester validation
            var result = promoCodeService.validate("123", new BigDecimal("50"));
            
            if (result.isPresent() && result.get().isValid()) {
                return ResponseEntity.ok("✅ SUCCÈS: Code 123 fonctionne! Réduction: " + 
                        result.get().getDiscountAmount() + "€");
            } else {
                return ResponseEntity.ok("❌ ÉCHEC: " + 
                        (result.isPresent() ? result.get().getMessage() : "Pas de réponse"));
            }
            
        } catch (Exception e) {
            log.error("Erreur test simple", e);
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }
}

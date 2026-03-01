package esprit.inscription.controller;
import esprit.inscription.entity.Cart;
import esprit.inscription.entity.Order;
import esprit.inscription.entity.PromoCode;
import esprit.inscription.repository.SubscriptionPlanRepository;
import esprit.inscription.service.CartService;
import esprit.inscription.service.EmailService;
import esprit.inscription.service.PromoCodeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
@RestController
@RequestMapping("/api/inscription/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class CartController {

    private final CartService cartService;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final EmailService emailService;
    private final PromoCodeService promoCodeService;

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("CartController is working!");
    }

    @PostMapping("/create/{userId}")
    public ResponseEntity<Cart> createCart(@PathVariable Long userId) {
        try {
            log.info("Creating cart for user ID: {}", userId);
            Cart cart = cartService.getOrCreateCart(userId);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            log.error("Error creating cart for user: {}", userId, e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/add-offer-test/{userId}/{planId}")
    public ResponseEntity<?> addOfferTest(@PathVariable Long userId, @PathVariable Long planId) {
        try {
            log.info("TEST: Adding offer {} to cart for user {}", planId, userId);
            Cart cart = cartService.addOfferToCart(userId, planId);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            log.error("TEST Error adding offer: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/check-plan/{planId}")
    public ResponseEntity<?> checkPlan(@PathVariable Long planId) {
        try {
            log.info("Checking plan existence: {}", planId);
            // Vérifier si le plan existe en base de données
            var plan = subscriptionPlanRepository.findById(planId);
            if (plan.isPresent()) {
                log.info("Plan found: {} - {}", plan.get().getName(), plan.get().getPrice());
                return ResponseEntity.ok(plan.get());
            } else {
                log.error("Plan NOT found: {}", planId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error checking plan: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/create-test-plan")
    public ResponseEntity<?> createTestPlan() {
        try {
            log.info("Creating test plan");
            var plan = esprit.inscription.entity.SubscriptionPlan.builder()
                    .planType("BASIC")
                    .name("Test Basic Plan")
                    .price(new BigDecimal("29.99"))
                    .currency("EUR")
                    .durationDays(30)
                    .description("Test plan for debugging")
                    .build();
            
            var saved = subscriptionPlanRepository.save(plan);
            log.info("Test plan created: {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error creating test plan: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestParam String toEmail) {
        try {
            log.info("=== TEST EMAIL ===");
            log.info("Sending test email to: {}", toEmail);
            
            // Créer un utilisateur de test
            var testUser = esprit.inscription.entity.User.builder()
                    .firstName("Test")
                    .lastName("User")
                    .email(toEmail)
                    .englishLevel("A1")
                    .subscriptionStatus("TRIAL")
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            
            // Envoyer l'email de bienvenue
            emailService.sendWelcomeEmail(testUser);
            log.info("Test email sent successfully to: {}", toEmail);
            return ResponseEntity.ok("Test email sent to: " + toEmail);
            
        } catch (Exception e) {
            log.error("Error sending test email: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test-promo")
    public ResponseEntity<String> testPromoCode(@RequestParam(defaultValue = "123") String code, 
                                           @RequestParam(defaultValue = "50") BigDecimal amount) {
        log.info("=== TEST AUTOMATIQUE CODE PROMO ===");
        log.info("Code: {}, Montant: {}€", code, amount);
        
        try {
            // Créer le code promo "123" s'il n'existe pas
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
            log.info("✅ Code promo '123' créé/vérifié");
            
            // Tester la validation
            var result = promoCodeService.validate(code, amount);
            
            if (result.isPresent()) {
                var validation = result.get();
                if (validation.isValid()) {
                    BigDecimal newTotal = amount.subtract(validation.getDiscountAmount());
                    log.info("✅ SUCCÈS: Code '{}' valide!", code);
                    log.info("   Montant original: {}€", amount);
                    log.info("   Réduction: {}€", validation.getDiscountAmount());
                    log.info("   Nouveau total: {}€", newTotal);
                    
                    return ResponseEntity.ok(String.format(
                        "✅ CODE PROMO VALIDE!\n" +
                        "Code: %s\n" +
                        "Montant: %.2f€\n" +
                        "Réduction: %.2f€\n" +
                        "Total: %.2f€", 
                        code, amount, validation.getDiscountAmount(), newTotal));
                } else {
                    log.warn("❌ ÉCHEC: Code invalide - {}", validation.getMessage());
                    return ResponseEntity.ok("❌ CODE INVALIDE: " + validation.getMessage());
                }
            } else {
                log.warn("❌ ÉCHEC: Pas de résultat retourné");
                return ResponseEntity.ok("❌ ERREUR: Pas de réponse du service");
            }
            
        } catch (Exception e) {
            log.error("❌ ERREUR CRITIQUE pendant le test", e);
            return ResponseEntity.status(500).body("❌ ERREUR: " + e.getMessage());
        }
    }

    @GetMapping("/default")
    public ResponseEntity<Cart> getDefaultCart() {
        try {
            log.info("Getting default cart for user ID 1");
            Cart cart = cartService.getOrCreateCart(1L);
            log.info("Default cart retrieved successfully: {}", cart.getId());
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            log.error("Error getting default cart", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCartByUserId(@PathVariable Long userId) {
        try {
            log.info("Getting cart for user ID: {}", userId);
            return cartService.getCartByUserId(userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.ok(cartService.getOrCreateCart(userId)));
        } catch (Exception e) {
            log.error("Error getting cart for user: {}", userId, e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/add-offer")
    public ResponseEntity<?> addOfferToCart(@RequestBody AddOfferRequest request) {
        log.info("=== ADD OFFER REQUEST ===");
        log.info("User ID: {}", request.getUserId());
        log.info("Plan ID: {}", request.getSubscriptionPlanId());
        
        if (request.getUserId() == null || request.getSubscriptionPlanId() == null) {
            log.error("Invalid request: userId or subscriptionPlanId is null");
            return ResponseEntity.badRequest().body("Invalid request: userId and subscriptionPlanId are required");
        }
        
        try {
            Cart cart = cartService.addOfferToCart(request.getUserId(), request.getSubscriptionPlanId());
            log.info("SUCCESS: Offer added to cart. Cart ID: {}, Total: {}", cart.getId(), cart.getTotalAmount());
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            log.error("RUNTIME ERROR adding offer to cart: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("UNEXPECTED ERROR adding offer to cart: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Unexpected error: " + e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> addItemToCart(@RequestBody AddItemRequest request) {
        log.info("Adding item to cart for user {}", request.getUserId());
        
        try {
            Cart cart = cartService.addItemToCart(
                    request.getUserId(),
                    request.getSubscriptionPlanId(),
                    request.getUnitPrice(),
                    request.getPlanName()
            );
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            log.error("Error adding item to cart", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /** Métier avancé : ajout au panier avec réduction frères/sœurs (logique métier côté backend). */
    @PostMapping("/add-with-sibling-discount")
    public ResponseEntity<?> addWithSiblingDiscount(@RequestBody AddWithSiblingDiscountRequest request) {
        if (request.getUserId() == null || request.getSubscriptionPlanId() == null || request.getSiblingCount() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "userId, subscriptionPlanId et siblingCount requis"));
        }
        if (request.getSiblingCount() < 2) {
            return ResponseEntity.badRequest().body(Map.of("message", "siblingCount doit être >= 2"));
        }
        try {
            Cart cart = cartService.addWithSiblingDiscount(
                    request.getUserId(),
                    request.getSubscriptionPlanId(),
                    request.getSiblingCount()
            );
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error adding with sibling discount", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Erreur lors de l'ajout au panier avec réduction fratrie."));
        }
    }

    /** Métier avancé : ajout au panier en tarif famille (N personnes). */
    @PostMapping("/add-with-family-rate")
    public ResponseEntity<?> addWithFamilyRate(@RequestBody AddWithFamilyRateRequest request) {
        if (request.getUserId() == null || request.getSubscriptionPlanId() == null || request.getNumberOfPeople() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "userId, subscriptionPlanId et numberOfPeople requis"));
        }
        if (request.getNumberOfPeople() < 2) {
            return ResponseEntity.badRequest().body(Map.of("message", "numberOfPeople doit être >= 2"));
        }
        try {
            Cart cart = cartService.addWithFamilyRate(
                    request.getUserId(),
                    request.getSubscriptionPlanId(),
                    request.getNumberOfPeople()
            );
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error adding with family rate", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Erreur lors de l'ajout au panier (tarif famille)."));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkoutCart(@RequestBody CheckoutRequest request) {
        log.info("Checking out cart for user {} with payment method: {}", request.getUserId(), request.getPaymentMethod());
        
        try {
            Order order = cartService.checkoutCart(
                    request.getUserId(),
                    request.getPaymentMethod(),
                    request.getPromoCode()
            );
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Error during checkout", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{userId}/total")
    public ResponseEntity<CartTotalResponse> getCartTotal(@PathVariable Long userId) {
        try {
            BigDecimal total = cartService.getCartTotal(userId);
            int itemCount = cartService.getCartItemCount(userId);
            return ResponseEntity.ok(new CartTotalResponse(total, itemCount));
        } catch (Exception e) {
            log.error("Error getting cart total", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/item/{itemId}/user/{userId}")
    public ResponseEntity<?> removeItemFromCart(@PathVariable Long userId,
                                                 @PathVariable Long itemId) {
        try {
            Cart cart = cartService.removeItemFromCart(userId, itemId);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            log.error("Error removing item from cart", e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/user/{userId}/plan/{subscriptionPlanId}")
    public ResponseEntity<?> removeItemByPlanId(@PathVariable Long userId,
                                                 @PathVariable Long subscriptionPlanId) {
        try {
            Cart cart = cartService.removeItemByPlanId(userId, subscriptionPlanId);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            log.error("Error removing item by plan ID", e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId) {
        try {
            cartService.clearCart(userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error clearing cart", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Data
    public static class AddOfferRequest {
        private Long userId;
        private Long subscriptionPlanId;
    }

    @Data
    public static class AddItemRequest {
        private Long userId;
        private Long subscriptionPlanId;
        private BigDecimal unitPrice;
        private String planName;
    }

    @Data
    public static class AddWithSiblingDiscountRequest {
        private Long userId;
        private Long subscriptionPlanId;
        private Integer siblingCount;
    }

    @Data
    public static class AddWithFamilyRateRequest {
        private Long userId;
        private Long subscriptionPlanId;
        private Integer numberOfPeople;
    }

    @Data
    public static class CheckoutRequest {
        private Long userId;
        private String paymentMethod;
        private String promoCode;
    }

    @Data
    public static class CartTotalResponse {
        private BigDecimal totalAmount;
        private Integer itemCount;

        public CartTotalResponse(BigDecimal totalAmount, Integer itemCount) {
            this.totalAmount = totalAmount;
            this.itemCount = itemCount;
        }
    }
}

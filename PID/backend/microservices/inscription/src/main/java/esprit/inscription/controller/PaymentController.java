package esprit.inscription.controller;

import esprit.inscription.entity.Payment;
import esprit.inscription.service.PaymentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stripe.exception.StripeException;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inscription/payment")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final esprit.inscription.service.RealTimeStatsService realTimeStatsService;

    @Value("${stripe.publishable-key:}")
    private String stripePublishableKey;

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable Long orderId) {
        return paymentService.getPaymentByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status));
    }

    @PostMapping("/process")
    public ResponseEntity<Payment> processPayment(@RequestBody ProcessPaymentRequest request) {
        try {
            Payment payment = paymentService.processPayment(
                    request.getOrderId(),
                    request.getAmount(),
                    request.getMethod());

            // Push real-time stats update to admin dashboard
            realTimeStatsService.pushStatsUpdate();

            // Send admin-only notification about the new payment
            String amountStr = request.getAmount() != null ? String.format("%.2f €", request.getAmount()) : "N/A";
            realTimeStatsService.sendAdminNotification(
                    "💳 New Payment Received!",
                    "Amount: " + amountStr + " — Method: " + request.getMethod() + " (Order #" + request.getOrderId()
                            + ")",
                    esprit.inscription.dto.NotificationDTO.Type.SUCCESS);

            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Payment> updatePaymentStatus(@PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        try {
            Payment payment = paymentService.updatePaymentStatus(id, request.getStatus());
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/verify/{userId}")
    public ResponseEntity<Boolean> verifyUserPayment(@PathVariable Long userId) {
        Boolean isPaid = paymentService.isUserPaid(userId);
        return ResponseEntity.ok(isPaid);
    }

    /**
     * Appelé au retour de Stripe (success URL) pour finaliser le paiement sans webhook (ex. dev local).
     */
    @PostMapping("/stripe/verify-session")
    public ResponseEntity<?> verifyStripeCheckoutSession(@RequestBody VerifyCheckoutSessionRequest request) {
        try {
            Payment payment = paymentService.verifyStripeCheckoutSession(
                    request.getSessionId(),
                    request.getExpectedOrderId());
            realTimeStatsService.pushStatsUpdate();
            return ResponseEntity.ok(payment);
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Stripe Checkout integration: returns a Stripe Checkout Session id.
     * Frontend will redirect to Stripe using Stripe.js.
     */
    @PostMapping("/stripe/checkout-session")
    public ResponseEntity<?> createStripeCheckoutSession(@RequestBody StripeCheckoutRequest request) {
        try {
            var session = paymentService.createStripeCheckoutSession(
                    request.getOrderId(),
                    request.getSuccessUrl(),
                    request.getCancelUrl(),
                    request.getLoyaltyPoints()
            );
            return ResponseEntity.ok(Map.of("sessionId", session.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Frontend-safe Stripe configuration.
     * Returns publishable key (pk_...) only (never returns secret key).
     */
    @GetMapping("/stripe/config")
    public ResponseEntity<?> getStripeConfig() {
        if (stripePublishableKey == null || stripePublishableKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Stripe publishable key is not configured. Set stripe.publishable-key property."));
        }
        if (!stripePublishableKey.startsWith("pk_")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Invalid Stripe publishable key. It must start with pk_."));
        }
        return ResponseEntity.ok(Map.of("publishableKey", stripePublishableKey));
    }

    /**
     * Stripe webhook: receives raw body and Stripe-Signature, verifies and marks payment completed.
     * Configure STRIPE_WEBHOOK_SECRET in env and point Stripe Dashboard to POST /api/inscription/payment/stripe/webhook.
     */
    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> stripeWebhook(HttpServletRequest request) {
        String payload;
        try {
            payload = request.getReader().lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Unable to read body");
        }
        String sigHeader = request.getHeader("Stripe-Signature");
        if (sigHeader == null || sigHeader.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing Stripe-Signature");
        }
        try {
            Payment payment = paymentService.handleStripeWebhookEvent(payload, sigHeader);

            if (payment != null) {
                // Push real-time stats update to admin dashboard
                realTimeStatsService.pushStatsUpdate();

                // Send admin-only notification about the new Stripe payment
                String amountStr = payment.getAmount() != null
                        ? String.format("%.2f %s", payment.getAmount(), "€")
                        : "N/A";
                realTimeStatsService.sendAdminNotification(
                        "💳 New Stripe Payment Received!",
                        "Amount: " + amountStr + " — Method: " + payment.getMethod()
                                + " (Order #" + payment.getOrderId() + ")",
                        esprit.inscription.dto.NotificationDTO.Type.SUCCESS);
            }
            return ResponseEntity.ok("OK");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Data
    public static class ProcessPaymentRequest {
        private Long orderId;
        private Double amount;
        private String method;
    }

    @Data
    public static class UpdateStatusRequest {
        private String status;
    }

    @Data
    public static class VerifyCheckoutSessionRequest {
        private String sessionId;
        private Long expectedOrderId;
    }

    @Data
    public static class StripeCheckoutRequest {
        private Long orderId;
        private String successUrl;
        private String cancelUrl;
        /**
         * Points de fidélité que le client souhaite utiliser pour cette commande.
         * Optionnel : si null ou <= 0, aucun rabais fidélité n'est appliqué.
         */
        private Long loyaltyPoints;
    }
}

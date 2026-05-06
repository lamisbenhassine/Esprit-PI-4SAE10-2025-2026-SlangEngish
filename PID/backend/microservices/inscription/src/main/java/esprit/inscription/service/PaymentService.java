package esprit.inscription.service;

import esprit.inscription.entity.Order;
import esprit.inscription.entity.Payment;
import esprit.inscription.repository.OrderRepository;
import esprit.inscription.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final LoyaltyService loyaltyService;
    private final OrderService orderService;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @Value("${stripe.currency:eur}")
    private String stripeCurrency;

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status);
    }

    @Transactional
    public Payment processPayment(Long orderId, Double amount, String method) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Check if payment already exists for this order
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent()) {
            throw new RuntimeException("Payment already exists for order: " + orderId);
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setMethod(method);
        payment.setStatus("pending");
        payment.setTransactionId(generateTransactionId());

        // Simulate payment processing
        // In a real application, this would integrate with a payment gateway
        payment.setStatus("completed");

        Payment saved = paymentRepository.save(payment);

        // Métier avancé 6 : crédits de points de fidélité après paiement simulé
        loyaltyService.addPointsForOrder(order.getUserId(), saved.getAmount(), orderId);

        return saved;
    }

    @Transactional
    public Payment updatePaymentStatus(Long paymentId, String status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));
        
        payment.setStatus(status);
        return paymentRepository.save(payment);
    }

    public Boolean isUserPaid(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        
        return orders.stream()
                .anyMatch(order -> {
                    Optional<Payment> payment = paymentRepository.findByOrderId(order.getId());
                    return payment.isPresent() && "completed".equals(payment.get().getStatus());
                });
    }

    /**
     * Create a Stripe Checkout Session for a given order.
     * IMPORTANT: you must set STRIPE_SECRET_KEY (or stripe.secret-key property) with your own key.
     * Optionally applies a loyalty discount (requestedPoints) to reduce the charged amount.
     */
    public Session createStripeCheckoutSession(Long orderId, String successUrl, String cancelUrl, Long loyaltyPoints) throws StripeException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        String secretKey = System.getenv("STRIPE_SECRET_KEY");
        if (secretKey == null || secretKey.isBlank()) secretKey = stripeSecretKey;
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "Stripe secret key is not configured. Set STRIPE_SECRET_KEY env var or stripe.secret-key property.");
        }
        if (!secretKey.startsWith("sk_")) {
            throw new IllegalStateException(
                    "Invalid Stripe secret key. Backend key must start with sk_ (not pk_ or rk_).");
        }
        Stripe.apiKey = secretKey;

        BigDecimal amount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        long appliedPoints = 0L;
        if (loyaltyPoints != null && loyaltyPoints > 0L) {
            var preview = loyaltyService.previewRedemption(order.getUserId(), amount, loyaltyPoints);
            if (preview != null && preview.getFinalTotal() != null) {
                amount = preview.getFinalTotal();
                appliedPoints = preview.getAppliedPoints() != null ? preview.getAppliedPoints() : 0L;
            }
        }
        // Stripe amounts are in the smallest currency unit (e.g. cents).
        long amountInCents = amount.multiply(BigDecimal.valueOf(100L)).longValue();

        String currency = stripeCurrency != null && !stripeCurrency.isBlank() ? stripeCurrency.toLowerCase() : "eur";

        SessionCreateParams.Builder builder =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .putMetadata("orderId", String.valueOf(orderId));

        if (loyaltyPoints != null && loyaltyPoints > 0L) {
            builder.putMetadata("loyaltyRequestedPoints", String.valueOf(loyaltyPoints));
            builder.putMetadata("loyaltyAppliedPoints", String.valueOf(appliedPoints));
        }

        SessionCreateParams params =
                builder.addLineItem(SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Order #" + order.getOrderNumber())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build())
                        .build();

        return Session.create(params);
    }

    /**
     * Handles Stripe webhook event (e.g. checkout.session.completed).
     * Verifies signature with STRIPE_WEBHOOK_SECRET and marks payment as completed.
     */
    @Transactional
    public Payment handleStripeWebhookEvent(String payload, String stripeSignature) {
        String webhookSecret = System.getenv("STRIPE_WEBHOOK_SECRET");
        if (webhookSecret == null || webhookSecret.isBlank()) webhookSecret = stripeWebhookSecret;
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException(
                    "Stripe webhook secret is not configured. Set STRIPE_WEBHOOK_SECRET env var or stripe.webhook-secret property.");
        }
        if (!webhookSecret.startsWith("whsec_")) {
            throw new IllegalStateException(
                    "Invalid Stripe webhook secret. It must start with whsec_.");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, stripeSignature, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Invalid Stripe webhook signature", e);
        }
        if (!"checkout.session.completed".equals(event.getType())) {
            return null;
        }
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) return null;
        Map<String, String> metadata = session.getMetadata();
        if (metadata == null || !metadata.containsKey("orderId")) return null;
        long orderId = Long.parseLong(metadata.get("orderId"));
        long appliedLoyaltyPoints = 0L;
        if (metadata.containsKey("loyaltyAppliedPoints")) {
            try {
                appliedLoyaltyPoints = Long.parseLong(metadata.get("loyaltyAppliedPoints"));
            } catch (NumberFormatException ignored) {
            }
        }
        String transactionId = session.getPaymentIntent() != null && !session.getPaymentIntent().isEmpty()
                ? session.getPaymentIntent() : session.getId();
        return markOrderPaymentCompletedFromStripe(orderId, transactionId, appliedLoyaltyPoints);
    }

    /**
     * Marks payment as completed for an order (create or update) after Stripe checkout.
     */
    @Transactional
    public Payment markOrderPaymentCompletedFromStripe(Long orderId, String stripeTransactionId, long appliedLoyaltyPoints) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        Optional<Payment> existing = paymentRepository.findByOrderId(orderId);
        boolean alreadyCompleted = existing.isPresent()
                && existing.get().getStatus() != null
                && "completed".equalsIgnoreCase(existing.get().getStatus().trim());
        Payment payment;
        if (existing.isPresent()) {
            payment = existing.get();
            payment.setStatus("completed");
            payment.setTransactionId(stripeTransactionId);
            payment.setMethod("Stripe");
        } else {
            payment = Payment.builder()
                    .orderId(orderId)
                    .amount(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
                    .method("Stripe")
                    .status("completed")
                    .transactionId(stripeTransactionId)
                    .build();
        }
        Payment saved = paymentRepository.save(payment);

        if (!alreadyCompleted) {
            // Métier avancé 6 : crédits de points de fidélité après paiement Stripe confirmé
            loyaltyService.addPointsForOrder(order.getUserId(), saved.getAmount(), orderId);

            // Débit réel des points utilisés si un palier a été appliqué
            if (appliedLoyaltyPoints > 0L) {
                loyaltyService.applyRedemption(order.getUserId(), orderId, appliedLoyaltyPoints);
            }

            // Envoi de l'email de confirmation d'achat après paiement Stripe confirmé
            try {
                orderService.sendPurchaseConfirmationEmailForOrder(orderId);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(PaymentService.class)
                        .warn("Failed to send purchase confirmation email for order {}", orderId, e);
            }
        }

        return saved;
    }

    /**
     * Après retour navigateur depuis Stripe : vérifie la session auprès de Stripe si le webhook n'est pas configuré.
     */
    @Transactional
    public Payment verifyStripeCheckoutSession(String sessionId, Long expectedOrderId) throws StripeException {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        String secretKey = System.getenv("STRIPE_SECRET_KEY");
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = stripeSecretKey;
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "Stripe secret key is not configured. Set STRIPE_SECRET_KEY env var or stripe.secret-key property.");
        }
        if (!secretKey.startsWith("sk_")) {
            throw new IllegalStateException(
                    "Invalid Stripe secret key. Backend key must start with sk_ (not pk_ or rk_).");
        }
        Stripe.apiKey = secretKey;

        Session session = Session.retrieve(sessionId);
        if (!"complete".equals(session.getStatus())) {
            throw new IllegalArgumentException("Checkout session is not complete");
        }
        if (!"paid".equals(session.getPaymentStatus())) {
            throw new IllegalArgumentException("Payment is not paid yet");
        }
        Map<String, String> metadata = session.getMetadata();
        if (metadata == null || !metadata.containsKey("orderId")) {
            throw new IllegalArgumentException("Missing orderId in Checkout Session metadata");
        }
        long orderId = Long.parseLong(metadata.get("orderId"));
        if (expectedOrderId != null && expectedOrderId > 0L && orderId != expectedOrderId) {
            throw new IllegalArgumentException("orderId does not match Checkout Session");
        }
        long appliedLoyaltyPoints = 0L;
        if (metadata.containsKey("loyaltyAppliedPoints")) {
            try {
                appliedLoyaltyPoints = Long.parseLong(metadata.get("loyaltyAppliedPoints"));
            } catch (NumberFormatException ignored) {
            }
        }
        String transactionId = session.getPaymentIntent() != null && !session.getPaymentIntent().isEmpty()
                ? session.getPaymentIntent()
                : session.getId();
        return markOrderPaymentCompletedFromStripe(orderId, transactionId, appliedLoyaltyPoints);
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}

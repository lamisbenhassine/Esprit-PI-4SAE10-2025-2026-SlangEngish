package esprit.inscription.service;

import esprit.inscription.entity.*;
import esprit.inscription.repository.OrderRepository;
import esprit.inscription.repository.UserRepository;
import esprit.inscription.service.PromoCodeService.PromoCodeValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.math.BigDecimal.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final PromoCodeService promoCodeService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional
    public Order createOrderFromCart(Long userId, String paymentMethod, String promoCode) {
        log.info("Creating order from cart for user {} with payment method: {}", userId, paymentMethod);
        
        Cart cart = cartService.getCartByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cannot create order: cart is empty for user " + userId);
        }

        BigDecimal rawTotal = cart.getTotalAmount() != null ? cart.getTotalAmount() : ZERO;
        BigDecimal discountAmount = ZERO;
        String appliedPromo = null;
        if (promoCode != null && !promoCode.isBlank()) {
            Optional<PromoCodeValidationResult> validation = promoCodeService.validate(promoCode.trim(), rawTotal);
            if (validation.isPresent() && validation.get().isValid()) {
                discountAmount = validation.get().getDiscountAmount();
                appliedPromo = validation.get().getCode();
            }
        }
        BigDecimal totalAmount = rawTotal.subtract(discountAmount);
        if (totalAmount.compareTo(ZERO) < 0) {
            totalAmount = ZERO;
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount.compareTo(ZERO) > 0 ? discountAmount : null);
        order.setPromoCode(appliedPromo);
        order.setPaymentMethod(paymentMethod);

        cart.getCartItems().forEach(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setSubscriptionPlanId(cartItem.getSubscriptionPlanId());
            orderItem.setQuantity(1);
            orderItem.setItemName(cartItem.getPlanName());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            order.addItem(orderItem);
        });

        Order savedOrder = orderRepository.save(order);
        if (appliedPromo != null) {
            promoCodeService.incrementUsage(appliedPromo);
        }
        cartService.clearCartContents(cart);

        // Envoyer l'email d'inscription (Slang English) dès la création de la commande pour tous les modes de paiement
        try {
            sendPurchaseConfirmationEmail(userId, savedOrder);
        } catch (Exception e) {
            log.error("Failed to send inscription email for order {}", savedOrder.getOrderNumber(), e);
        }

        log.info("Successfully created order {} for user {}", savedOrder.getOrderNumber(), userId);
        return savedOrder;
    }

    /**
     * Envoie l'email de confirmation d'achat pour une commande (appelé après confirmation paiement Stripe ou autre).
     */
    @Transactional(readOnly = true)
    public void sendPurchaseConfirmationEmailForOrder(Long orderId) {
        Order order = orderRepository.findByIdWithOrderItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        sendPurchaseConfirmationEmail(order.getUserId(), order);
    }

    /**
     * Envoie l'email d'inscription (Slang English) pour la dernière commande de l'utilisateur (pour test manuel).
     * @return l'adresse email à laquelle l'email a été envoyé si succès, empty si pas de commande
     */
    public Optional<String> sendInscriptionEmailForUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for id: " + userId));
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("User " + userId + " has no email address in database");
        }
        List<Order> orders = orderRepository.findByUserId(userId);
        if (orders == null || orders.isEmpty()) {
            log.warn("No order found for user {}, cannot send inscription email", userId);
            return Optional.empty();
        }
        Order lastOrder = orders.get(orders.size() - 1);
        try {
            sendPurchaseConfirmationEmail(userId, lastOrder);
            return Optional.of(user.getEmail());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to send inscription email for user {}", userId, e);
            throw new RuntimeException("Email send failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), e);
        }
    }

    private void sendPurchaseConfirmationEmail(Long userId, Order order) {
        log.info("Sending purchase confirmation email to user {} for order {}", userId, order != null ? order.getOrderNumber() : null);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        emailService.sendPurchaseConfirmationEmail(user, order);
        log.info("Purchase confirmation email sent successfully to user {}", userId);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }
}

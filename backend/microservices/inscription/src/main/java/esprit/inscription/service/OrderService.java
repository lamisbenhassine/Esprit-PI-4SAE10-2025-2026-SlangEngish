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
    private final EmailCampaignService emailCampaignService;

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

        BigDecimal rawTotal = cart.getTotalAmount();
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
        cartService.clearCart(userId);

        // Send purchase confirmation email
        sendPurchaseConfirmationEmail(userId, savedOrder);

        log.info("Successfully created order {} for user {}", savedOrder.getOrderNumber(), userId);
        return savedOrder;
    }

    private void sendPurchaseConfirmationEmail(Long userId, Order order) {
        log.info("Sending purchase confirmation email to user {} for order {}", userId, order.getOrderNumber());
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // Create purchase confirmation campaign
            EmailCampaign campaign = EmailCampaign.builder()
                    .name("Purchase Confirmation - " + order.getOrderNumber())
                    .category(EmailCampaign.CampaignCategory.PROMOTIONAL)
                    .subject("Purchase Confirmation - Order #" + order.getOrderNumber())
                    .fromEmail("orders@english-academy.com")
                    .fromName("English Academy")
                    .targetLevel(user.getEnglishLevel())
                    .build();

            EmailCampaign createdCampaign = emailCampaignService.createCampaign(campaign);
            
            // Launch campaign for this specific user
            emailCampaignService.launchCampaign(createdCampaign.getId());
            
            log.info("Purchase confirmation email sent successfully to user {}", userId);
        } catch (Exception e) {
            log.error("Failed to send purchase confirmation email to user {}", userId, e);
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }
}

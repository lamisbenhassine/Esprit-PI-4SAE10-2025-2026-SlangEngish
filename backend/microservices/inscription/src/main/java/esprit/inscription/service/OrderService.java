package esprit.inscription.service;

import esprit.inscription.entity.Cart;
import esprit.inscription.entity.Order;
import esprit.inscription.entity.OrderItem;
import esprit.inscription.repository.OrderRepository;
import esprit.inscription.service.PromoCodeService.PromoCodeValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final PromoCodeService promoCodeService;

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
        Cart cart = cartService.getCartByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cannot create order: cart is empty for user " + userId);
        }

        double rawTotal = cart.getTotalAmount();
        double discountAmount = 0.0;
        String appliedPromo = null;
        if (promoCode != null && !promoCode.isBlank()) {
            Optional<PromoCodeValidationResult> validation = promoCodeService.validate(promoCode.trim(), rawTotal);
            if (validation.isPresent() && validation.get().isValid()) {
                discountAmount = validation.get().getDiscountAmount();
                appliedPromo = validation.get().getCode();
            }
        }
        double totalAmount = Math.max(0, rawTotal - discountAmount);

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount > 0 ? discountAmount : null);
        order.setPromoCode(appliedPromo);
        order.setPaymentMethod(paymentMethod);

        cart.getCartItems().forEach(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setSubscriptionPlanId(cartItem.getSubscriptionPlanId());
            orderItem.setQuantity(1);
            orderItem.setItemName(cartItem.getPlanName());
            order.addItem(orderItem);
        });

        Order savedOrder = orderRepository.save(order);
        if (appliedPromo != null) {
            promoCodeService.incrementUsage(appliedPromo);
        }
        cartService.clearCart(userId);

        return savedOrder;
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }
}

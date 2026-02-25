package esprit.inscription.service;

import esprit.inscription.entity.Cart;
import esprit.inscription.entity.CartItem;
import esprit.inscription.repository.CartItemRepository;
import esprit.inscription.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    /** Returns the cart for the user; when multiple exist (legacy data), the latest by id (most likely to have items). */
    public Optional<Cart> getCartByUserId(Long userId) {
        return cartRepository.findFirstByUserIdOrderByIdDesc(userId);
    }

    /**
     * Get existing cart or create one. Handles race where another request already created a cart.
     */
    public Cart getOrCreateCart(Long userId) {
        Optional<Cart> existing = cartRepository.findFirstByUserIdOrderByIdDesc(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            newCart.setTotalAmount(0.0);
            return cartRepository.saveAndFlush(newCart);
        } catch (DataIntegrityViolationException e) {
            // Another request created the cart (unique user_id) — fetch and return it
            return cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Could not get or create cart for user: " + userId));
        }
    }

    @Transactional
    public Cart addItemToCart(Long userId, Long subscriptionPlanId, Double unitPrice, String planName) {
        Cart cart = getOrCreateCart(userId);

        // Prevent duplicate plans in cart
        boolean alreadyExists = cart.getCartItems().stream()
                .anyMatch(item -> item.getSubscriptionPlanId().equals(subscriptionPlanId));

        if (alreadyExists) {
            return cart;
        }

        CartItem item = new CartItem();
        item.setSubscriptionPlanId(subscriptionPlanId);
        item.setUnitPrice(unitPrice);
        item.setPlanName(planName);

        cart.addItem(item);
        cart = cartRepository.save(cart);
        cartRepository.flush(); // ensure items are persisted before response
        return cart;
    }

    @Transactional
    public Cart removeItemFromCart(Long userId, Long itemId) {
        Cart cart = cartRepository.findFirstByUserIdOrderByIdDesc(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        CartItem itemToRemove = cart.getCartItems().stream()
                .filter(item -> item.getId() != null && item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart: " + itemId));

        cart.removeItem(itemToRemove);
        cartItemRepository.delete(itemToRemove);
        return cartRepository.saveAndFlush(cart);
    }

    @Transactional
    public Cart removeItemByPlanId(Long userId, Long subscriptionPlanId) {
        Cart cart = cartRepository.findFirstByUserIdOrderByIdDesc(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        CartItem itemToRemove = cart.getCartItems().stream()
                .filter(item -> subscriptionPlanId.equals(item.getSubscriptionPlanId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart for plan: " + subscriptionPlanId));

        cart.removeItem(itemToRemove);
        cartItemRepository.delete(itemToRemove);
        return cartRepository.saveAndFlush(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findFirstByUserIdOrderByIdDesc(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        cart.getCartItems().clear();
        cart.setTotalAmount(0.0);
        cartRepository.save(cart);
    }
}

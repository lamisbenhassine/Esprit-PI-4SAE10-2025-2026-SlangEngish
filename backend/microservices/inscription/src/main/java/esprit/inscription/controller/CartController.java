package esprit.inscription.controller;

import esprit.inscription.entity.Cart;
import esprit.inscription.service.CartService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inscription/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCartByUserId(@PathVariable Long userId) {
        return cartService.getCartByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(cartService.getOrCreateCart(userId)));
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> addItemToCart(@RequestBody AddItemRequest request) {
        Cart cart = cartService.addItemToCart(
                request.getUserId(),
                request.getSubscriptionPlanId(),
                request.getUnitPrice(),
                request.getPlanName()
        );
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/item/{itemId}/user/{userId}")
    public ResponseEntity<?> removeItemFromCart(@PathVariable Long userId,
                                                 @PathVariable Long itemId) {
        try {
            Cart cart = cartService.removeItemFromCart(userId, itemId);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
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
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class AddItemRequest {
        private Long userId;
        private Long subscriptionPlanId;
        private Double unitPrice;
        private String planName;
    }
}

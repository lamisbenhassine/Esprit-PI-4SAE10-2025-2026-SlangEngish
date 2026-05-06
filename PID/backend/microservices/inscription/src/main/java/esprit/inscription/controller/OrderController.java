package esprit.inscription.controller;

import esprit.inscription.entity.Order;
import esprit.inscription.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inscription/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final esprit.inscription.service.RealTimeStatsService realTimeStatsService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable Long userId) {
        log.info("Getting orders for user: {}", userId);
        try {
            List<Order> orders = orderService.getOrdersByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error getting orders for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Envoi manuel de l'email d'inscription (Slang English) pour un utilisateur.
     * Chemin sous /test/ pour ne pas être confondu avec GET /{id}.
     * GET /api/inscription/orders/test/send-inscription-email?userId=1
     */
    @GetMapping("/test/send-inscription-email")
    public ResponseEntity<?> sendTestInscriptionEmail(@RequestParam Long userId) {
        try {
            java.util.Optional<String> sentTo = orderService.sendInscriptionEmailForUser(userId);
            if (sentTo.isPresent()) {
                return ResponseEntity.ok(java.util.Map.of(
                        "message", "Inscription email sent to user " + userId,
                        "sentTo", sentTo.get(),
                        "hint", "Check this inbox and your Spam / Promotions folder."));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "No order found for user " + userId));
        } catch (RuntimeException e) {
            log.warn("Test inscription email failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Request failed"));
        } catch (Exception e) {
            log.error("Error sending test inscription email for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Email send failed"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        log.info("Getting order by ID: {}", id);
        try {
            return orderService.getOrderById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting order by ID: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<Order> getOrderByOrderNumber(@PathVariable String orderNumber) {
        log.info("Getting order by number: {}", orderNumber);
        try {
            return orderService.getOrderByOrderNumber(orderNumber)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting order by number: {}", orderNumber, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createOrderFromCart(@RequestBody(required = false) CreateOrderRequest request) {
        if (request == null || request.getUserId() == null) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "userId est requis dans le corps JSON."));
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "paymentMethod est requis."));
        }

        log.info("Creating order from cart for user {} with payment method: {}", request.getUserId(), request.getPaymentMethod());

        final Order order;
        try {
            order = orderService.createOrderFromCart(
                    request.getUserId(),
                    request.getPaymentMethod(),
                    request.getPromoCode());
        } catch (Exception e) {
            log.error("Error creating order from cart for user: {}", request.getUserId(), e);
            return ResponseEntity.badRequest().body(java.util.Map.of("error", describeError(e)));
        }

        // Ne pas faire échouer la création de commande si WebSocket / stats échouent (broker indispo, sérialisation, etc.)
        try {
            realTimeStatsService.pushStatsUpdate();
            realTimeStatsService.sendAdminNotification(
                    "🛒 New Order Created",
                    "User #" + request.getUserId() + " placed a new order (Order #" + order.getOrderNumber() + ") — "
                            + request.getPaymentMethod(),
                    esprit.inscription.dto.NotificationDTO.Type.SUCCESS);
        } catch (Exception ex) {
            log.warn("Real-time stats/notification failed after order {} (order still created): {}",
                    order.getOrderNumber(), ex.getMessage());
        }

        log.info("Successfully created order {} for user {}", order.getOrderNumber(), request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("Deleting order: {}", id);
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting order: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Data
    public static class CreateOrderRequest {
        private Long userId;
        private String paymentMethod;
        private String promoCode;
    }

    private static String describeError(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable cur = e;
        int depth = 0;
        while (cur != null && depth < 5) {
            String m = cur.getMessage();
            if (m != null && !m.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                sb.append(m);
            }
            cur = cur.getCause();
            depth++;
        }
        if (sb.length() == 0) {
            sb.append(e.getClass().getSimpleName());
        }
        return sb.toString();
    }
}

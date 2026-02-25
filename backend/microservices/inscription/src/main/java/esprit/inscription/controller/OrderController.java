package esprit.inscription.controller;

import esprit.inscription.entity.Order;
import esprit.inscription.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inscription/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final esprit.inscription.service.RealTimeStatsService realTimeStatsService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<Order> getOrderByOrderNumber(@PathVariable String orderNumber) {
        return orderService.getOrderByOrderNumber(orderNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createOrderFromCart(@RequestBody CreateOrderRequest request) {
        try {
            Order order = orderService.createOrderFromCart(
                    request.getUserId(),
                    request.getPaymentMethod(),
                    request.getPromoCode());
            // Push stats update to admin dashboard
            realTimeStatsService.pushStatsUpdate();
            // Notify admin about new order
            realTimeStatsService.sendAdminNotification(
                    "🛒 New Order Created",
                    "User #" + request.getUserId() + " placed a new order (Order #" + order.getId() + ") — "
                            + request.getPaymentMethod(),
                    esprit.inscription.dto.NotificationDTO.Type.SUCCESS);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (RuntimeException e) {
            // Return error message in body so frontend can display the exact cause
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CreateOrderRequest {
        private Long userId;
        private String paymentMethod;
        private String promoCode;
    }
}

package esprit.inscription.controller;

import esprit.inscription.entity.Payment;
import esprit.inscription.service.PaymentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inscription/payment")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final esprit.inscription.service.RealTimeStatsService realTimeStatsService;

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
}
